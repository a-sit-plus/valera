package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.wallet.lib.jws.VerifyJwsObject
import at.asitplus.wallet.lib.jws.VerifyJwsObjectFun
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
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class TrustListService(
    private val persistentTrustListStore: PersistentTrustListStore,
    private val httpService: HttpService,
    private val verifyJwsObject: VerifyJwsObjectFun = VerifyJwsObject(),
) {
    private var job: Job? = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = httpService.buildHttpClient()

    private val defaultUrls = listOf(
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/pid-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/wallet-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/wrpac-providers.json",
        "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/mdl-providers.json",
        "https://trust.tech.ec.europa.eu/lists/eudiw/pub-eaa-providers.json"
    )

    /**
     * Starts the periodic background loop.
     * Default interval is 1 hour as configured.
     */
    fun startChecking(interval: Duration = 1.hours) {
        job?.cancel()

        job = scope.launch {
            delay(15.seconds)
            while (isActive) {
                refreshAll()
                delay(interval)
            }
        }
    }

    fun refreshAll(): Job = scope.launch {
        defaultUrls.forEach { url ->
            syncSingleUrl(url)
        }
    }

    private suspend fun syncSingleUrl(url: String) {
        fetchTrustList(url)
            .onSuccess { result ->
                persistentTrustListStore.persistTrustList(url, result.rawJwsText, Clock.System.now().epochSeconds)
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

        val (jwsCompact, payload) = JwsCompact.parse<TrustListPayload>(responseBody).getOrThrow()

        verifyJwsObject(jwsCompact).getOrThrow()

        Napier.i("Successfully validated Trust List signature from $url")

        TrustListResult(
            rawJwsText = responseBody,
            loTe = payload.loTe
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