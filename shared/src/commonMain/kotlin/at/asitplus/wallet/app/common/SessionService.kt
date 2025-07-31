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
    private val koin = getKoin()

    private val scopeId = MutableStateFlow(generateUuid())
    val scope = MutableStateFlow(initScope())


    @OptIn(ExperimentalUuidApi::class)
    fun generateUuid() = Uuid.random().toString()
    private fun initScope(): Scope {
        scopeId.value = generateUuid()
        return this.koin.createScope(scopeId.value, named(SESSION_NAME))
    }

    fun newScope() {
        koin.deleteScope(scopeId.value)
        scope.value = initScope()
    }
}