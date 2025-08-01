package at.asitplus.wallet.app.common

import at.asitplus.signum.indispensable.CryptoSignature
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.signum.supreme.os.PlatformSigningProviderSigner
import at.asitplus.wallet.lib.agent.KeyMaterial
import io.github.aakira.napier.Napier

open class WalletKeyMaterial(val keyMaterial: KeyMaterial) :
    KeyMaterial by keyMaterial {

    init {
        // Napier.i won't print anything to logcat, because it's not initialized yet
        println("Using this WalletKeyMaterial: ${joseCompliantSerializer.encodeToString(keyMaterial.jsonWebKey)}")
    }

    override suspend fun sign(
        data: ByteArray
    ): SignatureResult<CryptoSignature.RawByteEncodable> = run {
        when (val signer = keyMaterial.getUnderLyingSigner()) {
            is PlatformSigningProviderSigner<*, *> ->
                signer.sign(data) {
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
}