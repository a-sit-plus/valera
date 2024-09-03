package at.asitplus.wallet.app.common

import at.asitplus.catching
import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.KeyPairAdapter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class WalletCryptoService(private val defaultCryptoService: DefaultCryptoService) :
    CryptoService by defaultCryptoService {

    constructor(keyPairAdapter: KeyPairAdapter) : this(DefaultCryptoService(keyPairAdapter))

    var currentAuthorizationContext: CryptoServiceAuthorizationContext? = null

    override suspend fun doSign(input: ByteArray) = defaultCryptoService.doSign(input).also {
        when (it) {
            is SignatureResult.Error -> onSignError?.invoke()
            is SignatureResult.Failure -> onUnauthenticated?.invoke()
            is SignatureResult.Success -> onSuccess?.invoke()
        }
    }


    var onUnauthenticated: (() -> Unit)? = null
    var onSignError: (() -> Unit)? = null

    var onSuccess: (() -> Unit)? = null

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