package at.asitplus.wallet.app.common.presentation

import io.github.aakira.napier.Napier
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import ui.viewmodels.authentication.PresentationStateModel
import org.multipaz.util.UUID

enum class LocalPresentmentSource {
    IN_APP_QR,
    ANDROID_EXTERNAL_NFC,
}

enum class LocalPresentmentEngagementMethod {
    QR_CODE,
    NFC,
}

class LocalPresentmentBusyException(message: String) : IllegalStateException(message)

data class ActiveLocalPresentmentSession(
    val sessionId: String,
    val source: LocalPresentmentSource,
    val engagementMethod: LocalPresentmentEngagementMethod,
    val presentationStateModel: PresentationStateModel,
)

class LocalPresentmentSessionCoordinator {
    companion object {
        private const val TAG = "LocalPresentmentSessionCoordinator"
    }

    private class ManagedSession(
        val snapshot: ActiveLocalPresentmentSession,
        val cleanupCallbacks: MutableList<() -> Unit> = mutableListOf(),
        var uiAttached: Boolean = false,
    )

    private val stateLock = SynchronizedObject()
    private val coordinatorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName(TAG)
    )
    private var activeSession: ManagedSession? = null

    fun startSession(
        source: LocalPresentmentSource,
        engagementMethod: LocalPresentmentEngagementMethod,
    ): ActiveLocalPresentmentSession = synchronized(stateLock) {
        activeSession?.let { managed ->
            val state = managed.snapshot.presentationStateModel.state.value
            throw LocalPresentmentBusyException(
                "Local presentment already active " +
                        "sessionId=${managed.snapshot.sessionId} " +
                        "source=${managed.snapshot.source} state=$state"
            )
        }
        val sessionId = UUID.randomUUID().toString()
        val sessionScope = CoroutineScope(
            coordinatorScope.coroutineContext +
                    SupervisorJob(coordinatorScope.coroutineContext[Job]) +
                    CoroutineName("$TAG:$sessionId")
        )
        val snapshot = ActiveLocalPresentmentSession(
            sessionId = sessionId,
            source = source,
            engagementMethod = engagementMethod,
            presentationStateModel = PresentationStateModel(sessionScope)
        )
        activeSession = ManagedSession(snapshot = snapshot)
        Napier.d(
            "Started local presentment sessionId=$sessionId source=$source engagementMethod=$engagementMethod",
            tag = TAG
        )
        snapshot
    }

    fun activeSession(): ActiveLocalPresentmentSession? = synchronized(stateLock) {
        activeSession?.snapshot
    }

    fun activeSessionId(): String? = synchronized(stateLock) {
        activeSession?.snapshot?.sessionId
    }

    fun currentPresentationStateModel(): PresentationStateModel? = synchronized(stateLock) {
        activeSession?.snapshot?.presentationStateModel
    }

    fun currentPresentationStateModel(source: LocalPresentmentSource): PresentationStateModel? = synchronized(stateLock) {
        activeSession?.takeIf { it.snapshot.source == source }?.snapshot?.presentationStateModel
    }

    fun isSessionActive(sessionId: String): Boolean = synchronized(stateLock) {
        activeSession?.snapshot?.sessionId == sessionId
    }

    fun markUiAttached(sessionId: String) {
        synchronized(stateLock) {
            val managed = activeSession ?: return
            if (managed.snapshot.sessionId != sessionId) {
                return
            }
            managed.uiAttached = true
        }
        Napier.d("Attached UI to local presentment sessionId=$sessionId", tag = TAG)
    }

    fun registerCleanup(sessionId: String, cleanup: () -> Unit): Boolean = synchronized(stateLock) {
        val managed = activeSession ?: return false
        if (managed.snapshot.sessionId != sessionId) {
            return false
        }
        managed.cleanupCallbacks += cleanup
        true
    }

    fun finishSession(sessionId: String, reason: String) {
        val managed = synchronized(stateLock) {
            val current = activeSession
            if (current?.snapshot?.sessionId != sessionId) {
                return
            }
            activeSession = null
            current
        }
        val model = managed.snapshot.presentationStateModel
        val state = model.state.value
        if (state != PresentationStateModel.State.COMPLETED &&
            state != PresentationStateModel.State.IDLE
        ) {
            runCatching { model.reset() }
                .onFailure { Napier.w("Failed to reset sessionId=$sessionId during finish", it, tag = TAG) }
        }
        managed.cleanupCallbacks.forEach { cleanup ->
            runCatching(cleanup)
                .onFailure { Napier.w("Cleanup callback failed for sessionId=$sessionId", it, tag = TAG) }
        }
        Napier.d("Finished local presentment sessionId=$sessionId reason=$reason state=$state", tag = TAG)
    }

    fun resetAll(reason: String) {
        activeSessionId()?.let { finishSession(it, reason) }
    }
}
