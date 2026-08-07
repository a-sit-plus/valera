package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.indispensable.pki.leaf
import at.asitplus.wallet.lib.etsi.LoTEFilterCriteria
import at.asitplus.wallet.lib.etsi.LoTEFilterService
import at.asitplus.wallet.lib.etsi.LoTEServiceType
import at.asitplus.wallet.lib.etsi.isTrustedBy
import at.asitplus.wallet.lib.jws.VerifyJwsObjectFun
import at.asitplus.wallet.lib.jws.VerifyJwsObjectJades
import data.storage.DataStoreService
import data.storage.PersistentTrustListStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

class TrustListService(
    private val persistentTrustListStore: PersistentTrustListStore,
    httpService: HttpService,
    dataStoreService: DataStoreService,
    private val sessionCoroutineScope: CoroutineScope,
    private val verifyJwsObject: VerifyJwsObjectFun = VerifyJwsObjectJades(),
    private val clock: Clock = Clock.System,
) {
    private var job: Job? = null
    private val client = httpService.cachedResourceClient(dataStoreService, revalidate = true)

    // A-SIT trust list
    private val aistIssuerCert = X509Certificate.decodeFromPem(asitRootPem).getOrThrow()
    private val loTeFilterService: LoTEFilterService = LoTEFilterService()

    /**
     * Internal generic helper to observe a target flow alongside fresh trust lists.
     * Accepts a suspending evaluation lambda.
     */
    private fun <T : Any> combineWithFreshTrustStore(
        targetFlow: Flow<T?>,
        evaluate: suspend (T, Map<String, ListOfTrustedEntities>) -> TrustState
    ): Flow<TrustState> = combine(
        targetFlow,
        persistentTrustListStore.observeTrustContainer(LoTEServiceType.defaultUrls)
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

        evaluateIssuer(issuer, freshTrustLists, LoTEServiceType.fromSchemeIdentifier(schemeIdentifier))
    }


    /**
     * Evaluates if a given issuer is trusted based on the internal root cert and LoTEs.
     */
    fun evaluateIssuer(
        issuer: X509Certificate,
        trustLists: Map<String, ListOfTrustedEntities>,
        serviceType: LoTEServiceType
    ): TrustState = try {
        if (issuer.isTrustedBy(listOf(aistIssuerCert)).isSuccess) {
            TrustState.TRUSTED
        } else {
            val criteria = LoTEFilterCriteria(expectedServiceType = serviceType)
            val certificateList: List<X509Certificate> = trustLists
                .flatMap { (key, lote) -> loTeFilterService.extractTrustedCertificates(key, lote, criteria) }
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
        return evaluateIssuer(leaf, trustLists, LoTEServiceType.WRPAC)
    }

    /**
     * Flow variant, analogous to [observeTrustStateForEntry], for reactively evaluating a
     * relying party's trust state as fresh trust lists come in.
     */
    fun observeTrustStateForRelyingParty(
        requestFlow: Flow<RequestParametersFrom<*>?>
    ): Flow<TrustState> = combineWithFreshTrustStore(requestFlow) { request, freshTrustLists ->
        evaluateRelyingParty(request.extractRequesterCertificateChains(), freshTrustLists)
    }

    fun observeTrustStateForCertChain(
        certChainFlow: Flow<CertificateChain?>
    ): Flow<TrustState> = combineWithFreshTrustStore(certChainFlow) { certChain, freshTrustLists ->
        evaluateRelyingParty(certChain, freshTrustLists)
    }

    /** Refreshes missing or expired lists, then sleeps until the earliest cached list expires. */
    fun startChecking(retryInterval: Duration = 1.hours) {
        job?.cancel()

        job = sessionCoroutineScope.launch {
            delay(5.seconds)
            while (isActive) {
                val failed = refreshStaleEntries()
                val cachedAt = LoTEServiceType.defaultUrls
                    .mapNotNull { persistentTrustListStore.getCachedAt(it) }
                delay(
                    if (failed || cachedAt.size != LoTEServiceType.defaultUrls.size) retryInterval
                    else maxOf(
                        1.seconds,
                        cachedAt.nextRefreshIn(clock.now(), Configuration.CACHE_TTL_TRUST_LIST),
                    )
                )
            }
        }
    }

    private suspend fun refreshStaleEntries(): Boolean {
        val now = clock.now()
        return LoTEServiceType.defaultUrls
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

/** Keeps only cache entries younger than [ttl], dropping the timestamp. Generic so it is trivially testable. */
internal fun <T> Map<String, Pair<T, Instant>>.filterFresh(now: Instant, ttl: Duration): Map<String, T> =
    filterValues { now - it.second < ttl }.mapValues { it.value.first }

internal fun Collection<Instant>.nextRefreshIn(now: Instant, ttl: Duration): Duration =
    minOfOrNull { it + ttl - now }?.let { maxOf(Duration.ZERO, it) } ?: Duration.ZERO


fun RequestParametersFrom<*>.extractRequesterCertificateChains(): List<X509Certificate>? =
    when (this) {
        is RequestParametersFrom.Jws<*> ->
            (this.jws as JwsCompact).jwsHeader.certificateChain

        is RequestParametersFrom.OpenId4VpDcApiSigned ->
            this.jwsTyped.jws.jwsHeader.certificateChain

        is RequestParametersFrom.OpenId4VpDcApiMultiSigned ->
            this.jwsTyped.jws.jwsHeaders.firstOrNull()?.certificateChain

        is RequestParametersFrom.Uri,
        is RequestParametersFrom.Json,
        is RequestParametersFrom.OpenId4VpDcApiUnsigned -> emptyList()
        is RequestParametersFrom.IsoMdocDcApi -> extractRelyingPartyCertificateChains()
    }

fun RequestParametersFrom.IsoMdocDcApi.extractRelyingPartyCertificateChains(): List<X509Certificate> {
    val deviceRequest = this.parameters.isoMdocRequest.deviceRequest

    val fromReaderAuthAll = deviceRequest.readerAuthAll
        ?.firstOrNull()?.unprotectedHeader?.certificateChain?.map { cert -> X509Certificate.decodeFromDer(cert) }
        ?: emptyList()

    return fromReaderAuthAll
}
