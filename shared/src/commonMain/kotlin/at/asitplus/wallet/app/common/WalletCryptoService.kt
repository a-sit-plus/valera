package at.asitplus.wallet.app.common

import at.asitplus.signum.indispensable.CryptoSignature
import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.signum.supreme.asKmmResult
import at.asitplus.signum.supreme.os.PlatformSigningProviderSigner
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.KeyMaterial

open class WalletCryptoService(private val defaultCryptoService: DefaultCryptoService) :
    CryptoService by defaultCryptoService {

    constructor(keyMaterial: KeyMaterial) : this(DefaultCryptoService(keyMaterial))

    override suspend fun sign(
        input: ByteArray
    ): SignatureResult<CryptoSignature.RawByteEncodable> = defaultCryptoService.run {
         when (val signer = keyMaterial.getUnderLyingSigner()) {
            is PlatformSigningProviderSigner<*> -> signer.sign(input) {
                unlockPrompt {
                    promptText?.let { message = it }
                    promptCancelText?.let { cancelText = it }
                }
            }

            else -> signer.sign(input)
        }.also {
            when (it) {
                is SignatureResult.Error -> onSignError?.invoke()
                is SignatureResult.Failure -> onUnauthenticated?.invoke()
                is SignatureResult.Success -> onSuccess?.invoke()
            }
        }
    }


    var onUnauthenticated: (() -> Unit)? = null
    var onSignError: (() -> Unit)? = null

    var onSuccess: (() -> Unit)? = null

    var promptText: String? = null
    var promptCancelText: String? = null
    var promptSubtitle: String? = null

}