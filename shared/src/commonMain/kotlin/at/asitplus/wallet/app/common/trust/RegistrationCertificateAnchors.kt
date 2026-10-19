package at.asitplus.wallet.app.common.trust

import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.io.ByteArrayBase64Serializer
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.valera.resources.Res
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.nextRefreshIn
import at.asitplus.wallet.lib.agent.TrustedCertificates
import data.storage.DataStoreService
import data.storage.PersistentHttpCacheStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Trust anchors of the providers of wallet-relying party registration certificates (WRPRC), which are not published
 * as a List of Trusted Entities: the root certificates bundled with the app in [BUNDLED_ROOTS], and those loaded from
 * the URL configured in [SettingsRepository.wrprcTrustAnchorsUrl], see [parseRegistrationTrustAnchors] for its format.
 *
 * Like the Lists of Trusted Entities, the loaded file is persisted and used offline for
 * [Configuration.CACHE_TTL_TRUST_LIST], refreshed in the background by [keepFresh], and loaded again on demand by
 * [refresh].
 */
class RegistrationCertificateAnchors(
    private val httpService: HttpService,
    private val dataStoreService: DataStoreService,
    settingsRepository: SettingsRepository,
    private val bundledPem: suspend () -> String = { Res.readBytes(BUNDLED_ROOTS).decodeToString() },
    private val clock: Clock = Clock.System,
) {
    private val client = httpService.cachedResourceClient(dataStoreService, revalidate = true)

    // Same storage the client above caches into, to drop the raw response of a URL that is no longer configured.
    private val cachedResponses = PersistentHttpCacheStorage(dataStoreService, Configuration.DATASTORE_KEY_HTTP_CACHE)
    private val mutex = Mutex()
    private var bundled: Set<X509Certificate>? = null

    // Parsed anchors of the stored file, so that every validation does not parse the archive again.
    private var parsed: Pair<StoredAnchors, RegistrationTrustAnchors>? = null

    private val url: Flow<String?> = settingsRepository.wrprcTrustAnchorsUrl
        .map { url -> url?.takeIf { it.isNotBlank() } }
        .distinctUntilChanged()

    /** For the registration certificates of relying parties and credential issuers. */
    val trustedCertificates = TrustedCertificates { bundledCertificates() + configured().providers }

    /** For the status lists of registration certificates: those of the dedicated signers, and of the providers. */
    val statusCertificates = TrustedCertificates {
        bundledCertificates() + configured().let { it.providers + it.statusListSigners }
    }

    /**
     * Loads the anchors from the configured URL if they are missing or expired, or with [force] in any case, then
     * without a conditional request, i.e. downloading the whole file again.
     *
     * @return whether fresh anchors are available, `true` if no URL is configured
     */
    suspend fun refresh(force: Boolean = false): Boolean {
        val url = url.first() ?: return true
        return mutex.withLock {
            loadStored(url)?.takeIf { !force && it.isFresh() } != null || download(url, force) != null
        }
    }

    /**
     * Keeps the anchors of the configured URL fresh, as [at.asitplus.wallet.app.common.TrustListService.startChecking]
     * keeps the lists: loads missing or expired ones, then sleeps until they expire, or for [retryInterval] after a
     * failure. Drops the anchors of a URL that is no longer configured.
     */
    suspend fun keepFresh(retryInterval: Duration) {
        url.collectLatest { url ->
            pruneUnless(url)
            if (url == null) return@collectLatest
            while (currentCoroutineContext().isActive) {
                val fresh = refresh()
                val cachedAt = loadStored(url)?.cachedAt
                delay(
                    if (!fresh || cachedAt == null) retryInterval
                    else maxOf(1.seconds, listOf(cachedAt).nextRefreshIn(clock.now(), Configuration.CACHE_TTL_TRUST_LIST))
                )
            }
        }
    }

    private suspend fun bundledCertificates(): Set<X509Certificate> = bundled ?: catchingUnwrapped {
        parsePemCertificates(bundledPem())
    }.getOrElse {
        Napier.w("Could not load bundled WRPRC trust anchors", it)
        emptySet()
    }.also { bundled = it }

    /** The anchors of the configured URL, loaded if they are missing or expired. */
    private suspend fun configured(): RegistrationTrustAnchors {
        val url = url.first() ?: return RegistrationTrustAnchors.EMPTY
        val stored = mutex.withLock { loadStored(url)?.takeIf { it.isFresh() } ?: download(url, force = false) }
            ?: return RegistrationTrustAnchors.EMPTY
        parsed?.takeIf { (key, _) -> key.sameAs(stored) }?.let { return it.second }
        return catchingUnwrapped { parseRegistrationTrustAnchors(stored.content) }
            .getOrElse { RegistrationTrustAnchors.EMPTY }
            .also { parsed = stored to it }
    }

    private suspend fun download(url: String, force: Boolean): StoredAnchors? = catching {
        // A client of its own bypasses the HTTP cache and the cooldown after failures of the shared one.
        val content = if (force) {
            val client = httpService.buildHttpClient()
            try {
                fetch(client, url)
            } finally {
                client.close()
            }
        } else {
            fetch(client, url)
        }
        val anchors = parseRegistrationTrustAnchors(content)
        require(anchors.providers.isNotEmpty()) { "No certificates of WRPRC providers in $url" }
        StoredAnchors(url, content, clock.now().toEpochMilliseconds()).also {
            persist(it)
            parsed = it to anchors
            Napier.i(
                "Loaded ${anchors.providers.size} WRPRC provider and " +
                        "${anchors.statusListSigners.size} status list signer certificates from $url"
            )
        }
    }.onFailure {
        Napier.w("Could not load WRPRC trust anchors from $url", it)
    }.getOrNull()

    private suspend fun fetch(client: HttpClient, url: String): ByteArray {
        val response = client.get(url)
        val body = response.bodyAsBytes()
        require(response.status.isSuccess()) { "Loading WRPRC trust anchors failed with ${response.status}" }
        return body
    }

    /** Removes the stored anchors, and their cached HTTP response, unless they were loaded from [url]. */
    private suspend fun pruneUnless(url: String?) {
        val stored = loadStored() ?: return
        if (stored.url == url) return
        catchingUnwrapped {
            dataStoreService.deletePreference(Configuration.DATASTORE_KEY_WRPRC_TRUST_ANCHORS)
            parsed = null
            val cachedUrl = Url(stored.url)
            if (cachedResponses.findAll(cachedUrl).isNotEmpty()) {
                cachedResponses.removeAll(cachedUrl)
            }
            Napier.i("Removed WRPRC trust anchors of a URL that is no longer configured: ${stored.url}")
        }.onFailure {
            Napier.w("Could not remove WRPRC trust anchors of ${stored.url}", it)
        }
    }

    private suspend fun loadStored(url: String): StoredAnchors? = loadStored()?.takeIf { it.url == url }

    // Anchors stored by an earlier version, as a PEM bundle without `content`, fail to decode and are loaded again.
    private suspend fun loadStored(): StoredAnchors? =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_WRPRC_TRUST_ANCHORS).first()?.let {
            catchingUnwrapped { joseCompliantSerializer.decodeFromString<StoredAnchors>(it) }.getOrNull()
        }

    private suspend fun persist(anchors: StoredAnchors) = dataStoreService.setPreference(
        joseCompliantSerializer.encodeToString(anchors),
        Configuration.DATASTORE_KEY_WRPRC_TRUST_ANCHORS,
    )

    private fun StoredAnchors.isFresh() = clock.now() - cachedAt < Configuration.CACHE_TTL_TRUST_LIST

    /** The file loaded from [url], as it was loaded. */
    @Serializable
    private class StoredAnchors(
        val url: String,
        @Serializable(with = ByteArrayBase64Serializer::class)
        val content: ByteArray,
        val cachedAtEpochMillis: Long,
    ) {
        val cachedAt: Instant
            get() = Instant.fromEpochMilliseconds(cachedAtEpochMillis)

        fun sameAs(other: StoredAnchors) = url == other.url && cachedAtEpochMillis == other.cachedAtEpochMillis
    }

    companion object {
        /** Root certificates of WRPRC providers bundled with the app, as a PEM bundle. Test anchors only. */
        const val BUNDLED_ROOTS = "files/trust/wrprc-roots.pem"

        /** Folder of a ZIP archive with the certificates of the WRPRC providers. */
        const val PROVIDER_FOLDER = "WRPRC Provider CA Certificates"

        /** Folder of a ZIP archive with the certificates of the signers of status lists of WRPRCs. */
        const val STATUS_LIST_SIGNER_FOLDER = "WRPRC Revocation List Signing Certificates"
    }
}

/**
 * Anchors for registration certificates (WRPRC): the certificates of their [providers], and those of the
 * [statusListSigners] of their status lists, which only sign status lists of registration certificates.
 */
data class RegistrationTrustAnchors(
    val providers: Set<X509Certificate>,
    val statusListSigners: Set<X509Certificate>,
) {
    companion object {
        val EMPTY = RegistrationTrustAnchors(emptySet(), emptySet())
    }
}

/**
 * Anchors from [content], either a ZIP archive, e.g. the one of the EUDI Wallets Launchpad, with the certificates of
 * the providers in the folder [RegistrationCertificateAnchors.PROVIDER_FOLDER], and of the signers of status lists
 * in [RegistrationCertificateAnchors.STATUS_LIST_SIGNER_FOLDER], or else certificates of providers only, see
 * [parseCertificates]. Files in other folders are ignored, as are files that hold no certificate.
 */
internal fun parseRegistrationTrustAnchors(content: ByteArray): RegistrationTrustAnchors {
    if (!ZipArchive.isZip(content)) return RegistrationTrustAnchors(parseCertificates(content), emptySet())
    val providers = mutableSetOf<X509Certificate>()
    val statusListSigners = mutableSetOf<X509Certificate>()
    ZipArchive.files(content).forEach { (path, file) ->
        val folders = path.split('/').dropLast(1).map { it.trim() }
        val target = when {
            folders.any { it.equals(RegistrationCertificateAnchors.PROVIDER_FOLDER, ignoreCase = true) } -> providers
            folders.any { it.equals(RegistrationCertificateAnchors.STATUS_LIST_SIGNER_FOLDER, ignoreCase = true) } ->
                statusListSigners

            else -> return@forEach
        }
        catchingUnwrapped { parseCertificates(file) }
            .onSuccess { target += it }
            .onFailure { Napier.w("No certificate in $path of WRPRC trust anchors", it) }
    }
    return RegistrationTrustAnchors(providers, statusListSigners)
}

/** The certificates in [content]: a PEM bundle, see [parsePemCertificates], or a single DER encoded certificate. */
internal fun parseCertificates(content: ByteArray): Set<X509Certificate> {
    val pem = content.decodeToString()
    return if (PEM_CERTIFICATE.containsMatchIn(pem)) parsePemCertificates(pem)
    else setOf(X509Certificate.decodeFromDer(content))
}

/** Every certificate in [pem], a PEM bundle that may contain other text between its certificates. */
internal fun parsePemCertificates(pem: String): Set<X509Certificate> =
    PEM_CERTIFICATE.findAll(pem).mapTo(mutableSetOf()) { X509Certificate.decodeFromPem(it.value).getOrThrow() }

private val PEM_CERTIFICATE = Regex("-----BEGIN CERTIFICATE-----[\\s\\S]*?-----END CERTIFICATE-----")
