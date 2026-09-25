package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.iso.DeviceRequest
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsFlattened
import at.asitplus.signum.indispensable.josef.JwsGeneral
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.indispensable.pki.leaf
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.etsi.LoTEFilterService
import at.asitplus.wallet.lib.etsi.LoTEStage
import at.asitplus.wallet.lib.etsi.LoteProfile
import at.asitplus.wallet.lib.etsi.isTrustedBy
import at.asitplus.wallet.lib.jws.VerifyJwsObjectFun
import at.asitplus.wallet.lib.jws.VerifyJwsObjectJades
import data.storage.DataStoreService
import data.storage.PersistentHttpCacheStorage
import data.storage.PersistentTrustListStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ui.composables.TrustState
import ui.models.ResolvedCredential
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


val asitRootPem = "-----BEGIN CERTIFICATE-----\n" +
        "MIICNzCCAd6gAwIBAgIUVKbs5o5e1jnILQPrKrsBnZbJj5EwCgYIKoZIzj0EAwIw\n" +
        "MTELMAkGA1UEBhMCQVQxDjAMBgNVBAoMBUEtU0lUMRIwEAYDVQQDDAlJQUNBIDIw\n" +
        "MjYwHhcNMjYwNDE2MTQ1NDQ1WhcNMjcwNDE2MTQ1NDQ1WjAxMQswCQYDVQQGEwJB\n" +
        "VDEOMAwGA1UECgwFQS1TSVQxEjAQBgNVBAMMCUlBQ0EgMjAyNjBZMBMGByqGSM49\n" +
        "AgEGCCqGSM49AwEHA0IABA7215fpBuEqE0AmnwgUoKMGCIZjnXMPZohMJKKrO0f/\n" +
        "84eg4bFLVUAM25Clukqbjr/Ol3Pa16LLhxQoSIupJx+jgdMwgdAwEgYDVR0TAQH/\n" +
        "BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwMQYDVR0fBCowKDAmoCSgIoYgaHR0\n" +
        "cDovL3dhbGxldC5hLXNpdC5hdC9jcmwvMS5jcmwwIgYDVR0SBBswGYYXaHR0cHM6\n" +
        "Ly93YWxsZXQuYS1zaXQuYXQwEwYDVR0gBAwwCjAIBgYEAI96AQEwHwYDVR0jBBgw\n" +
        "FoAUTXNbbT6FjuThGuNsHM5KMNSead4wHQYDVR0OBBYEFE1zW20+hY7k4RrjbBzO\n" +
        "SjDUnmneMAoGCCqGSM49BAMCA0cAMEQCIDMQ328z1NWGUK6wcLC8JmgTkKxt3Ycw\n" +
        "BapSKA9Qxhd6AiANUlRcM5BT5JKZL3yNSvUlERYXqcEYs50sxwE60SVkEw==\n" +
        "-----END CERTIFICATE-----\n"

@OptIn(ExperimentalCoroutinesApi::class)
class TrustListService(
    private val persistentTrustListStore: PersistentTrustListStore,
    httpService: HttpService,
    dataStoreService: DataStoreService,
    settingsRepository: SettingsRepository,
    private val sessionCoroutineScope: CoroutineScope,
    private val verifyJwsObject: VerifyJwsObjectFun = VerifyJwsObjectJades(),
    private val clock: Clock = Clock.System,
) {
    private var job: Job? = null
    private val client = httpService.cachedResourceClient(dataStoreService, revalidate = true)

    // Same storage the client above caches into, to drop the raw responses of disabled stages as well.
    private val cachedResponses = PersistentHttpCacheStorage(dataStoreService, Configuration.DATASTORE_KEY_HTTP_CACHE)

    // A-SIT trust list
    private val aistIssuerCert = X509Certificate.decodeFromPem(asitRootPem).getOrThrow()
    private val loTeFilterService: LoTEFilterService = LoTEFilterService()

    /** Every list of every trust infrastructure stage the user has enabled. */
    private val trustListUrls: Flow<List<String>> = settingsRepository.trustListStages
        .map { stages -> LoteProfile.fetchUrls(stages) }
        .distinctUntilChanged()

    /**
     * Internal generic helper to observe a target flow alongside fresh trust lists.
     * Accepts a suspending evaluation lambda.
     */
    private fun <T : Any> combineWithFreshTrustStore(
        targetFlow: Flow<T?>,
        evaluate: suspend (T, Map<String, ListOfTrustedEntities>) -> TrustState
    ): Flow<TrustState> = combine(
        targetFlow,
        trustListUrls.flatMapLatest { persistentTrustListStore.observeTrustContainer(it) },
    ) { target, trustLists ->
        if (target == null) return@combine TrustState.EVALUATING

        val freshTrustLists = trustLists.filterFresh(clock.now(), Configuration.CACHE_TTL_TRUST_LIST)
        evaluate(target, freshTrustLists)
    }


    fun observeTrustStateForEntry(
        storeEntryFlow: Flow<ResolvedCredential?>
    ): Flow<TrustState> = combineWithFreshTrustStore(storeEntryFlow) { credential, freshTrustLists ->
        val entry = credential.entry
        val issuer = entry.issuer
            ?: return@combineWithFreshTrustStore TrustState.UNKNOWN

        // Works because the lambda is now suspend-aware!
        val scheme = entry.resolveScheme()
        val schemeIdentifier = entry.schemeIdentifier
            ?: scheme.vcType
            ?: scheme.sdJwtType
            ?: scheme.isoDocType

        if (schemeIdentifier.isNullOrBlank()) {
            return@combineWithFreshTrustStore TrustState.UNKNOWN
        }

        evaluateCertificate(issuer, freshTrustLists, LoteProfile.fromSchemeIdentifier(schemeIdentifier))
    }


    /**
     * Evaluates if a given issuer is trusted based on the internal root cert and LoTEs.
     *
     * Lists not matching [profile] contribute no certificates, so passing every cached list of every
     * enabled stage is fine.
     */
    fun evaluateCertificate(
        issuer: X509Certificate,
        trustLists: Map<String, ListOfTrustedEntities>,
        profile: LoteProfile,
    ): TrustState = try {
        if (issuer.isTrustedBy(listOf(aistIssuerCert)).isSuccess) {
            TrustState.TRUSTED
        } else {
            val certificateList: List<X509Certificate> = trustLists.values
                .flatMap { lote -> loTeFilterService.extractIssuanceCertificates(lote, profile) }
                .mapNotNull { it.certificate }

            if (certificateList.isEmpty()) {
                TrustState.UNTRUSTED
            } else if (issuer.isTrustedBy(certificateList).isSuccess) {
                TrustState.TRUSTED
            } else {
                TrustState.UNTRUSTED
            }
        }
    } catch (e: Exception) {
        Napier.e("Failed to evaluate issuer trust status due to unexpected error", e)
        TrustState.UNKNOWN
    }

    /**
     * Evaluates trust for the relying party (requester) behind a given request, using its
     * leaf certificate from the [RequestParametersFrom] chain, checked against the internal
     * A-SIT root and any WRPAC(Wallet Relying Party Access Certificate) entries in [trustLists].
     */
    fun evaluateRelyingParty(
        relyingPartyCertChain: CertificateChain?,
        trustLists: Map<String, ListOfTrustedEntities>,
    ): TrustState {
        val leaf = relyingPartyCertChain?.leaf
            ?: return TrustState.UNKNOWN
        return evaluateCertificate(leaf, trustLists, LoteProfile.WRPAC)
    }

    /**
     * Flow variant, analogous to [observeTrustStateForEntry], for reactively evaluating a
     * relying party's trust state as fresh trust lists come in.
     */
    fun observeTrustStateForRelyingParty(
        requestFlow: Flow<RequestParametersFrom<*>?>
    ): Flow<TrustState> = combineWithFreshTrustStore(requestFlow) { request, freshTrustLists ->
        evaluateRelyingParty(request.extractRelyingPartyCertificateChains(), freshTrustLists)
    }

    fun observeTrustStateForCertChain(
        certChainFlow: Flow<CertificateChain?>
    ): Flow<TrustState> = combineWithFreshTrustStore(certChainFlow) { certChain, freshTrustLists ->
        evaluateRelyingParty(certChain, freshTrustLists)
    }

    /**
     * Refreshes missing or expired lists, then sleeps until the earliest cached list expires.
     * Restarts whenever the user enables or disables a trust infrastructure stage.
     */
    fun startChecking(retryInterval: Duration = 1.hours) {
        job?.cancel()

        job = sessionCoroutineScope.launch {
            delay(5.seconds)
            trustListUrls.collectLatest { urls ->
                pruneTrustListsOfDisabledStages(urls)
                if (urls.isEmpty()) return@collectLatest
                while (isActive) {
                    val failed = refreshStaleEntries(urls)
                    val cachedAt = urls.mapNotNull { persistentTrustListStore.getCachedAt(it) }
                    delay(
                        if (failed || cachedAt.size != urls.size) retryInterval
                        else maxOf(
                            1.seconds,
                            cachedAt.nextRefreshIn(clock.now(), Configuration.CACHE_TTL_TRUST_LIST),
                        )
                    )
                }
            }
        }
    }

    /**
     * Removes the persisted lists, and their cached HTTP responses, of every stage that is not
     * enabled, so that disabling a stage really drops its data instead of leaving it behind for
     * the next session.
     */
    private suspend fun pruneTrustListsOfDisabledStages(enabledUrls: List<String>) {
        disabledTrustListUrls(enabledUrls).forEach { url ->
            catchingUnwrapped {
                if (persistentTrustListStore.removeTrustList(url)) {
                    Napier.i("Removed cached Trust List of a disabled stage: $url")
                }
                val cachedUrl = Url(url)
                if (cachedResponses.findAll(cachedUrl).isNotEmpty()) {
                    cachedResponses.removeAll(cachedUrl)
                }
            }.onFailure { e ->
                Napier.w("Could not remove cached Trust List: $url", e)
            }
        }
    }

    private suspend fun refreshStaleEntries(urls: List<String>): Boolean {
        val now = clock.now()
        return urls
            .filter { url ->
                val cachedAt = persistentTrustListStore.getCachedAt(url)
                cachedAt == null || now - cachedAt >= Configuration.CACHE_TTL_TRUST_LIST
            }
            .map { syncSingleUrl(it) }
            .any { !it }
    }

    private suspend fun syncSingleUrl(url: String): Boolean = catching {
        val rawJwsText = fetchTrustList(url).getOrThrow()
        persistentTrustListStore.persistTrustList(url, rawJwsText, clock.now())
        Napier.i("Successfully synced and persisted Trust List: $url")
    }.onFailure { e ->
        Napier.e("Background sync failed for Trust List: $url", e)
    }.isSuccess

    /**
     * Fetches the signed List of Trusted Entities (LoTE)
     * Returns the raw signed payload after parsing and signature verification.
     */
    suspend fun fetchTrustList(url: String): KmmResult<String> = catching {
        Napier.i("Fetching Trust List from: $url")
        val response = client.get(url) {
            accept(ContentType.Application.Json)
        }
        val responseBody = response.bodyAsText()
        val jws = JwsCompact.parse<TrustListPayload>(responseBody).getOrThrow()
        verifyJwsObject(jws.first).getOrThrow()
        Napier.i("Successfully validated Trust List signature from $url")
        responseBody
    }
}

/** Every trust list URL of every known stage that [enabledUrls] does not cover. */
internal fun disabledTrustListUrls(enabledUrls: Collection<String>): List<String> =
    LoTEStage.entries.flatMap { it.fetchUrls }.filterNot { it in enabledUrls }

/** Keeps only cache entries younger than [ttl], dropping the timestamp. Generic so it is trivially testable. */
internal fun <T> Map<String, Pair<T, Instant>>.filterFresh(now: Instant, ttl: Duration): Map<String, T> =
    filterValues { now - it.second < ttl }.mapValues { it.value.first }

internal fun Collection<Instant>.nextRefreshIn(now: Instant, ttl: Duration): Duration =
    minOfOrNull { it + ttl - now }?.let { maxOf(Duration.ZERO, it) } ?: Duration.ZERO


fun RequestParametersFrom<*>.extractRelyingPartyCertificateChains(): List<X509Certificate>? =
    when (this) {
        is RequestParametersFrom.Jws<*> -> when (val jws = this.jws) {
            is JwsCompact -> jws.jwsHeader.certificateChain
            is JwsFlattened -> jws.jwsHeader.certificateChain
            is JwsGeneral -> jws.jwsHeaders.firstNotNullOfOrNull { it.certificateChain }
        }
        is RequestParametersFrom.OpenId4VpDcApiSigned ->
            this.jwsTyped.jws.jwsHeader.certificateChain
        is RequestParametersFrom.OpenId4VpDcApiMultiSigned ->
            this.jwsTyped.jws.jwsHeaders.firstOrNull()?.certificateChain
        is RequestParametersFrom.Uri,
        is RequestParametersFrom.Json,
        is RequestParametersFrom.OpenId4VpDcApiUnsigned -> null
        is RequestParametersFrom.IsoMdocDcApi ->
            this.parameters.isoMdocRequest.deviceRequest.extractCertificateChain()
    }?.takeIf { it.isNotEmpty() }

fun DeviceRequest.extractCertificateChain(): List<X509Certificate>? =
    (readerAuthAll?.firstOrNull() ?: docRequests.firstNotNullOfOrNull { it.readerAuth })
        ?.let { cose ->
            cose.protectedHeader.certificateChain
                ?: cose.unprotectedHeader?.certificateChain
        }
        ?.mapNotNull { bytes ->
            runCatching { X509Certificate.decodeFromDer(bytes) }.getOrNull()
        }
        ?.takeIf { it.isNotEmpty() }