package at.asitplus.wallet.app.common.trust

import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.valera.resources.Res
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.TrustedCertificates
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Trust anchors of the providers of wallet-relying party registration certificates (WRPRC), which are not published
 * as a List of Trusted Entities: the root certificates bundled with the app in [BUNDLED_ROOTS], and those loaded from
 * the URL the user configured in [SettingsRepository.wrprcTrustAnchorsUrl], as a PEM bundle.
 *
 * Loaded certificates are persisted, and used offline for [Configuration.CACHE_TTL_TRUST_LIST], as trust lists are.
 */
class RegistrationCertificateAnchors(
    httpService: HttpService,
    private val dataStoreService: DataStoreService,
    private val settingsRepository: SettingsRepository,
    private val bundledPem: suspend () -> String = { Res.readBytes(BUNDLED_ROOTS).decodeToString() },
    private val clock: Clock = Clock.System,
) {
    private val client = httpService.cachedResourceClient(dataStoreService, revalidate = true)
    private val mutex = Mutex()
    private var bundled: Set<X509Certificate>? = null

    /** For the registration certificates of relying parties and credential issuers, and their status lists. */
    val trustedCertificates = TrustedCertificates { certificates() }

    suspend fun certificates(): Set<X509Certificate> = bundledCertificates() + configuredCertificates()

    private suspend fun bundledCertificates(): Set<X509Certificate> = bundled ?: catchingUnwrapped {
        parsePemCertificates(bundledPem())
    }.getOrElse {
        Napier.w("Could not load bundled WRPRC trust anchors", it)
        emptySet()
    }.also { bundled = it }

    private suspend fun configuredCertificates(): Set<X509Certificate> {
        val url = settingsRepository.wrprcTrustAnchorsUrl.first()?.takeIf { it.isNotBlank() }
            ?: return emptySet()
        val pem: String? = mutex.withLock {
            val now = clock.now()
            loadStored()?.takeIf { it.url == url && now - it.cachedAt < Configuration.CACHE_TTL_TRUST_LIST }?.pem
                ?: fetch(url).onSuccess { persist(url, it, now) }.onFailure {
                    Napier.w("Could not load WRPRC trust anchors from $url", it)
                }.getOrNull()
        }
        return pem?.let { catchingUnwrapped { parsePemCertificates(it) }.getOrNull() }.orEmpty()
    }

    private suspend fun fetch(url: String) = catching {
        val response = client.get(url)
        val body = response.bodyAsText()
        require(response.status.isSuccess()) { "Loading WRPRC trust anchors failed with ${response.status}" }
        require(parsePemCertificates(body).isNotEmpty()) { "No certificates in WRPRC trust anchors from $url" }
        body
    }

    private suspend fun loadStored(): StoredAnchors? =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_WRPRC_TRUST_ANCHORS).first()?.let {
            catchingUnwrapped { joseCompliantSerializer.decodeFromString<StoredAnchors>(it) }.getOrNull()
        }

    private suspend fun persist(url: String, pem: String, cachedAt: Instant) = dataStoreService.setPreference(
        joseCompliantSerializer.encodeToString(StoredAnchors(url, pem, cachedAt.toEpochMilliseconds())),
        Configuration.DATASTORE_KEY_WRPRC_TRUST_ANCHORS,
    )

    @Serializable
    private data class StoredAnchors(val url: String, val pem: String, val cachedAtEpochMillis: Long) {
        val cachedAt: Instant
            get() = Instant.fromEpochMilliseconds(cachedAtEpochMillis)
    }

    companion object {
        /** Root certificates of WRPRC providers bundled with the app, as a PEM bundle. Test anchors only. */
        const val BUNDLED_ROOTS = "files/trust/wrprc-roots.pem"
    }
}

/** Every certificate in [pem], a PEM bundle that may contain other text between its certificates. */
internal fun parsePemCertificates(pem: String): Set<X509Certificate> =
    PEM_CERTIFICATE.findAll(pem).mapTo(mutableSetOf()) { X509Certificate.decodeFromPem(it.value).getOrThrow() }

private val PEM_CERTIFICATE = Regex("-----BEGIN CERTIFICATE-----[\\s\\S]*?-----END CERTIFICATE-----")
