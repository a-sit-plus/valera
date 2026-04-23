package at.asitplus.wallet.app.common.fiissuer

import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Serializable
private data class PendingFIIssuerRequest(
    val transactionId: String,
    val credentialType: String,
    val claims: Map<String, String> = emptyMap(),
    val attemptsDone: Int = 0,
    val submittedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
)

private const val FIIssuerApprovedMessage = "FIIssuer credential approved and added to your wallet."
private const val FIIssuerRejectedMessage = "FIIssuer request was rejected."
private const val FIIssuerExpiredMessage = "FIIssuer request expired before approval."
private const val FIIssuerTimedOutMessage = "FIIssuer request timed out before approval."

class FIIssuerPollingService(
    private val dataStoreService: DataStoreService,
    private val fiIssuerService: FIIssuerService,
    private val holderAgent: HolderAgent,
    private val errorService: ErrorService,
    private val snackbarService: SnackbarService,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("FIIssuerPollingService")
    )
    private val jobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()

    fun resumePendingRequests() {
        scope.launch {
            loadPendingRequests().forEach { startPolling(it) }
        }
    }

    fun trackPendingRequest(
        transactionId: String,
        credentialType: String,
        claims: Map<String, String> = emptyMap(),
    ) {
        scope.launch {
            val pendingRequest = PendingFIIssuerRequest(
                transactionId = transactionId,
                credentialType = credentialType,
                claims = claims,
            )
            savePendingRequest(pendingRequest)
            startPolling(pendingRequest)
        }
    }

    suspend fun reset() {
        mutex.withLock {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_FIIssuer_PENDING_REQUESTS)
    }

    private suspend fun startPolling(pendingRequest: PendingFIIssuerRequest) {
        mutex.withLock {
            if (jobs[pendingRequest.transactionId]?.isActive == true) return
            jobs[pendingRequest.transactionId] = scope.launch {
                pollUntilResolved(pendingRequest)
            }
        }
    }

    private suspend fun pollUntilResolved(initialRequest: PendingFIIssuerRequest) {
        var currentRequest = initialRequest
        while (isPollingActive() && currentRequest.attemptsDone < 10) {
            val nextAttempt = currentRequest.attemptsDone + 1
            currentRequest = currentRequest.copy(attemptsDone = nextAttempt)
            savePendingRequest(currentRequest)

            runCatching {
                fiIssuerService.getCredentialOffer(currentRequest.transactionId)
            }.onFailure { error ->
                Napier.w(
                    "FIIssuer: polling failed for transactionId=${currentRequest.transactionId} attempt=$nextAttempt",
                    error,
                )
                errorService.emit(error)
                if (isPollingActive() && nextAttempt < 10) {
                    delay(1.minutes)
                }
            }.onSuccess { offer ->
                Napier.d(
                    "FIIssuer: polled transactionId=${currentRequest.transactionId} status=${offer.status} attempt=$nextAttempt"
                )
                when (offer.status) {
                    FIIssuerCredentialRequestStatus.PENDING -> {
                        if (nextAttempt < 10) {
                            delay(1.minutes)
                        } else {
                            clearPendingRequest(currentRequest.transactionId)
                            snackbarService.showSnackbar(FIIssuerTimedOutMessage)
                        }
                    }

                    FIIssuerCredentialRequestStatus.APPROVED -> {
                        val credential = offer.credential
                        if (credential.isNullOrBlank()) {
                            clearPendingRequest(currentRequest.transactionId)
                            errorService.emit(IllegalStateException("FIIssuer approved without credential payload"))
                            return
                        }
                        importApprovedCredential(credential)
                        clearPendingRequest(currentRequest.transactionId)
                        snackbarService.showSnackbar(FIIssuerApprovedMessage)
                        return
                    }

                    FIIssuerCredentialRequestStatus.REJECTED -> {
                        clearPendingRequest(currentRequest.transactionId)
                        snackbarService.showSnackbar(FIIssuerRejectedMessage)
                        return
                    }

                    FIIssuerCredentialRequestStatus.EXPIRED -> {
                        clearPendingRequest(currentRequest.transactionId)
                        snackbarService.showSnackbar(FIIssuerExpiredMessage)
                        return
                    }
                }
            }
        }

        if (isPollingActive()) {
            clearPendingRequest(initialRequest.transactionId)
            snackbarService.showSnackbar(FIIssuerTimedOutMessage)
        }
    }

    private suspend fun isPollingActive(): Boolean = currentCoroutineContext()[Job]?.isActive == true

    private suspend fun importApprovedCredential(serializedCredential: String) {
        runCatching {
            val issuedCredential = vckJsonSerializer.decodeFromString<Issuer.IssuedCredential>(serializedCredential)
            holderAgent.storeCredential(issuedCredential.toStoreCredentialInput())
        }.onFailure { error ->
            errorService.emit(error)
            throw error
        }
    }

    private suspend fun loadPendingRequests(): List<PendingFIIssuerRequest> = runCatching {
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_FIIssuer_PENDING_REQUESTS)
            .firstOrNull()
            ?.let { vckJsonSerializer.decodeFromString<List<PendingFIIssuerRequest>>(it) }
            .orEmpty()
    }.getOrElse {
        Napier.w("FIIssuer: failed to load pending requests", it)
        emptyList<PendingFIIssuerRequest>()
    }

    private suspend fun savePendingRequest(pendingRequest: PendingFIIssuerRequest) {
        val pendingRequests = loadPendingRequests().filterNot { it.transactionId == pendingRequest.transactionId } + pendingRequest
        dataStoreService.setPreference(
            key = Configuration.DATASTORE_KEY_FIIssuer_PENDING_REQUESTS,
            value = vckJsonSerializer.encodeToString(pendingRequests),
        )
    }

    private suspend fun clearPendingRequest(transactionId: String) {
        val remaining = loadPendingRequests().filterNot { it.transactionId == transactionId }
        if (remaining.isEmpty()) {
            dataStoreService.deletePreference(Configuration.DATASTORE_KEY_FIIssuer_PENDING_REQUESTS)
        } else {
            dataStoreService.setPreference(
                key = Configuration.DATASTORE_KEY_FIIssuer_PENDING_REQUESTS,
                value = vckJsonSerializer.encodeToString(remaining),
            )
        }
        mutex.withLock { jobs.remove(transactionId) }
    }
}










