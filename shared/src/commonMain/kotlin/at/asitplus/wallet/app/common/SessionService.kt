package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.scope.Scope

const val SESSION_NAME = "WALLET_SESSION"

data class SessionHandle(
    val scope: Scope,
    /** Called before [scope] is closed; use to cancel coroutines or release activity-held resources. */
    val onClose: () -> Unit = {},
)

/**
 * Manages one activity-local Koin scope and recreates it on soft/full resets.
 */
class SessionService(
) {
    private lateinit var scopeFactory: () -> SessionHandle
    private var onReset: () -> Unit = {}
    private lateinit var currentSessionHandle: SessionHandle
    private lateinit var _scope: MutableStateFlow<Scope>
    val scope: MutableStateFlow<Scope>
        get() = _scope

    /** Two-phase init: [scopeFactory] often captures [SessionService] itself, so it cannot be a constructor parameter. */
    fun initialize(onReset: () -> Unit = {}, scopeFactory: () -> SessionHandle) {
        this.scopeFactory = scopeFactory
        this.onReset = onReset
        currentSessionHandle = scopeFactory()
        _scope = MutableStateFlow(currentSessionHandle.scope)
    }

    /** Creates a new scope before closing the old one to avoid a window with no active scope. */
    fun newScope() {
        check(::scopeFactory.isInitialized) { "SessionService not initialized" }
        check(::currentSessionHandle.isInitialized) { "SessionService not initialized" }
        // onReset() cleans up caller-owned state (pending links, intentState, etc.).
        // Run it in a runCatching so a failure there does not abort scope recreation and
        // leave the session stuck with a closed or stale Koin scope.
        runCatching { onReset() }.onFailure { Napier.e("onReset threw during session reset", it) }
        val previousSessionHandle = currentSessionHandle
        currentSessionHandle = scopeFactory()
        scope.value = currentSessionHandle.scope
        closeSession(previousSessionHandle)
    }

    /** Closes the current session; safe to call if [initialize] was never called. */
    fun close() {
        if (!::currentSessionHandle.isInitialized) {
            return
        }
        closeSession(currentSessionHandle)
    }

    private fun closeSession(sessionHandle: SessionHandle) {
        sessionHandle.onClose()
        if (!sessionHandle.scope.closed) {
            sessionHandle.scope.close()
        }
    }
}
