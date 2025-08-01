package at.asitplus.wallet.app.common

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val SESSION_NAME = "WALLET_SESSION"
/**
 * Manages creation and deletion of Koin scopes.
 * Allows to reinitialize singleton dependencies e.g. on App reset
 */
class SessionService(): KoinComponent {
    private var scopeId = generateUuid()
    val scope = MutableStateFlow(initScope())

    @OptIn(ExperimentalUuidApi::class)
    private fun generateUuid() = Uuid.random().toString()

    private fun initScope(): Scope {
        scopeId = generateUuid()
        return getKoin().createScope(scopeId, named(SESSION_NAME))
    }

    fun newScope() {
        getKoin().deleteScope(scopeId)
        scope.value = initScope()
    }
}