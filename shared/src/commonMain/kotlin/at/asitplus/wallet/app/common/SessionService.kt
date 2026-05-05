package at.asitplus.wallet.app.common

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.scope.Scope

const val SESSION_NAME = "WALLET_SESSION"

data class SessionHandle(
    val scope: Scope,
    val onClose: () -> Unit = {},
)

/**
 * Manages one activity-local Koin scope and recreates it on soft/full resets.
 */
class SessionService(
) {
    private lateinit var scopeFactory: () -> SessionHandle
    private lateinit var currentSessionHandle: SessionHandle
    private lateinit var _scope: MutableStateFlow<Scope>
    val scope: MutableStateFlow<Scope>
        get() = _scope

    fun initialize(scopeFactory: () -> SessionHandle) {
        this.scopeFactory = scopeFactory
        currentSessionHandle = scopeFactory()
        _scope = MutableStateFlow(currentSessionHandle.scope)
    }

    fun newScope() {
        check(::scopeFactory.isInitialized) { "SessionService not initialized" }
        check(::currentSessionHandle.isInitialized) { "SessionService not initialized" }
        val previousSessionHandle = currentSessionHandle
        currentSessionHandle = scopeFactory()
        scope.value = currentSessionHandle.scope
        closeSession(previousSessionHandle)
    }

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
