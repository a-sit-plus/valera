package at.asitplus.wallet.app.common

import at.asitplus.signum.indispensable.CryptoSignature
import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.signum.supreme.os.PlatformSigningProviderSigner
import at.asitplus.wallet.lib.agent.KeyMaterial

open class WalletKeyMaterial(val keyMaterial: KeyMaterial) :
    KeyMaterial by keyMaterial {

    override suspend fun sign(
        data: ByteArray
    ): SignatureResult<CryptoSignature.RawByteEncodable> = run {
         when (val signer = keyMaterial.getUnderLyingSigner()) {
            is PlatformSigningProviderSigner<*, *> -> signer.sign(data) {
                unlockPrompt {
                    promptText?.let { message = it }
                    promptCancelText?.let { cancelText = it }
                }
            }

            else -> signer.sign(data)
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