package at.asitplus.wallet.app.common

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.scope.Scope

const val SESSION_NAME = "WALLET_SESSION"
/**
 * Manages one activity-local Koin scope and recreates it on soft/full resets.
 */
class SessionService(
) {
    private lateinit var scopeFactory: () -> Scope
    private lateinit var _scope: MutableStateFlow<Scope>
    val scope: MutableStateFlow<Scope>
        get() = _scope

    fun initialize(scopeFactory: () -> Scope) {
        this.scopeFactory = scopeFactory
        _scope = MutableStateFlow(scopeFactory())
    }

    fun newScope() {
        check(::scopeFactory.isInitialized) { "SessionService not initialized" }
        check(::_scope.isInitialized) { "SessionService not initialized" }
        val previousScope = scope.value
        scope.value = scopeFactory()
        if (!previousScope.closed) {
            previousScope.close()
        }
    }

    fun close() {
        if (!::_scope.isInitialized) {
            return
        }
        val currentScope = scope.value
        if (!currentScope.closed) {
            currentScope.close()
        }
    }
}
