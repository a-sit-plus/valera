package at.asitplus.wallet.app.common

import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.KeyPairAdapter
import kotlinx.coroutines.sync.Mutex

open class WalletCryptoService(private val defaultCryptoService: DefaultCryptoService) :
    CryptoService by defaultCryptoService {

    constructor(keyPairAdapter: KeyPairAdapter) : this(DefaultCryptoService(keyPairAdapter))

    override suspend fun doSign(
        input: ByteArray,
        promptText: String?,
        cancelText: String?
    ): SignatureResult = defaultCryptoService.doSign(
        input,
        promptInfo //TODO SUBTITLE
    ).also {
        when (it) {
            is SignatureResult.Error -> onSignError?.invoke()
            is SignatureResult.Failure -> onUnauthenticated?.invoke()
            is SignatureResult.Success -> onSuccess?.invoke()
        }
    }


    var onUnauthenticated: (() -> Unit)? = null
    var onSignError: (() -> Unit)? = null

    var onSuccess: (() -> Unit)? = null

    var promptInfo:String? =null

}