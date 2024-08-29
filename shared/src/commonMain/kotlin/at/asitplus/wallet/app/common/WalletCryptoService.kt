package at.asitplus.wallet.app.common

import at.asitplus.catching
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class WalletCryptoService(private val defaultCryptoService: DefaultCryptoService) : CryptoService by defaultCryptoService{
    var currentAuthorizationContext: CryptoServiceAuthorizationContext? = null
    private var authorizationPromptMutex = Mutex()
    open suspend fun useAuthorizationContext(
        context: CryptoServiceAuthorizationContext,
        block: suspend () -> Unit,
    ) = catching {
        authorizationPromptMutex.withLock {
            currentAuthorizationContext = context
            block()
            currentAuthorizationContext = null
        }
    }
}