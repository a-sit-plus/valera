package at.asitplus.wallet.app.android

import at.asitplus.signum.indispensable.CryptoSignature
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.signum.supreme.os.AndroidKeystoreSigner
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.lib.agent.KeyMaterial
import io.github.aakira.napier.Napier

class AndroidKeyMaterial(keyMaterial: KeyMaterial) : WalletKeyMaterial(keyMaterial) {

    override suspend fun sign(data: ByteArray): SignatureResult<CryptoSignature.RawByteEncodable> =
        with(keyMaterial.getUnderLyingSigner()) {
            if (this is AndroidKeystoreSigner) {
                //This is just here to pass the prompt message
                this.sign(data) {
                    unlockPrompt {
                        promptText?.let { message = it }
                        promptSubtitle?.let { subtitle = it }
                        promptCancelText?.let { cancelText = it }
                    }
                }
            } else super.sign(data)
        }
}

