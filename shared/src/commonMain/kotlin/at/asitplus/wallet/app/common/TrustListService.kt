package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.etsi.LoTEFilterCriteria
import at.asitplus.wallet.lib.etsi.LoTEFilterService
import at.asitplus.wallet.lib.etsi.isTrustedBy
import at.asitplus.wallet.lib.jws.VerifyJwsObjectFun
import at.asitplus.wallet.lib.jws.VerifyJwsObjectJades
import data.storage.PersistentTrustListStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ui.composables.TrustState
import ui.models.ResolvedCredential
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


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
    private val sessionCoroutineScope: CoroutineScope,
    private val verifyJwsObject: VerifyJwsObjectFun = VerifyJwsObjectJades(),
) {
    private var job: Job? = null
    private val client = httpService.buildHttpClient()
    // A-SIT trust list
    private val aistIssuerCert = X509Certificate.decodeFromPem(asitRootPem).getOrThrow()
    private val loTeFilterService: LoTEFilterService = LoTEFilterService()


    private val defaultUrls = listOf(
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/pid-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/wallet-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/wrpac-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/mdl-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/pub-eaa-providers.json"
    )

    fun observeTrustStateForEntry(
        storeEntryFlow: Flow<ResolvedCredential?>
    ): Flow<TrustState> =
        combine(
            storeEntryFlow,
            persistentTrustListStore.observeTrustContainer()
        ) { credential, trustContainerMap ->
            val entry = credential?.entry ?: return@combine TrustState.EVALUATING

            val issuerBytes = entry.issuer ?: return@combine TrustState.UNKNOWN
            val allLoTes = trustContainerMap.values.toList()
            val scheme = entry.resolveScheme()

            val serviceType = scheme.vcType
                ?: scheme.sdJwtType
                ?: scheme.isoDocType

            if (serviceType.isNullOrBlank()) return@combine TrustState.UNKNOWN

            evaluateIssuer(issuerBytes, allLoTes, serviceType)
        }


    /**
     * Evaluates if a given issuer is trusted based on the internal root cert and LoTEs.
     */
    fun evaluateIssuer(
        issuer: X509Certificate,
        trustLists: List<ListOfTrustedEntities>,
        serviceType: String
    ): TrustState = try {
            if (issuer.isTrustedBy(listOf(aistIssuerCert)).isSuccess) {
                return TrustState.TRUSTED
            }

            val criteria = LoTEFilterCriteria(expectedServiceType = serviceType)
            val certificateList: List<X509Certificate> = trustLists
                .flatMap { lote -> loTeFilterService.extractTrustedCertificates(lote, criteria) }
                .mapNotNull { it.certificate }

            if (certificateList.isEmpty()) {
                return TrustState.UNTRUSTED
            }

            val validationResult = issuer.isTrustedBy(certificateList)

            if (validationResult.isSuccess) TrustState.TRUSTED else TrustState.UNTRUSTED
        } catch (e: Exception) {
            Napier.e("Failed to evaluate issuer trust status due to unexpected error", e)
            TrustState.UNKNOWN
        }


    /**
     * Starts the periodic background loop.
     * Default interval is 1 hour as configured.
     */
    fun startChecking(interval: Duration = 1.hours) {
        job?.cancel()

        job = sessionCoroutineScope.launch {
            delay(5.seconds)
            while (isActive) {
                refreshAll()
                delay(interval)
            }
        }
    }

    fun refreshAll(): Job = sessionCoroutineScope.launch {
        defaultUrls.forEach { url ->
            syncSingleUrl(url)
        }
    }

    private suspend fun syncSingleUrl(url: String) {
        fetchTrustList(url)
            .onSuccess { result ->
                persistentTrustListStore.persistTrustList(url, result.rawJwsText)
                Napier.i("Successfully synced and persisted Trust List: $url")
            }
            .onFailure { e ->
                Napier.e("Background sync failed for Trust List: $url", e)
            }
    }

    /**
     * Fetches the signed List of Trusted Entities (LoTE)
     * Returns a [KmmResult] wrapping a [TrustListResult] containing both raw and parsed data.
     */
    suspend fun fetchTrustList(url: String): KmmResult<TrustListResult> = catching {
        Napier.i("Fetching Trust List from: $url")
        val response = client.get(url) {
            accept(ContentType.Application.Json)
        }
        val responseBody = response.bodyAsText()
        val jws = JwsCompact.parse<TrustListPayload>(responseBody).getOrThrow()
        verifyJwsObject(jws.first).getOrThrow()
        Napier.i("Successfully validated Trust List signature from $url")
        TrustListResult(
            rawJwsText = responseBody,
            loTe = jws.second.loTe
        )
    }
}

/**
 * Data container wrapping both the raw string representation for persistent caching
 * and the verified domain object.
 */
data class TrustListResult(
    val rawJwsText: String,
    val loTe: ListOfTrustedEntities
)