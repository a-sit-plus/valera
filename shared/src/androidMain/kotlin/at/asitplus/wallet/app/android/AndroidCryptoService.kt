package at.asitplus.wallet.app.android

import at.asitplus.KmmResult
import at.asitplus.signum.indispensable.josef.JweEncryption
import at.asitplus.signum.indispensable.josef.jcaKeySpecName
import at.asitplus.signum.indispensable.josef.jcaName
import at.asitplus.signum.supreme.os.AndroidKeystoreSigner
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.lib.agent.AuthenticatedCiphertext
import at.asitplus.wallet.lib.agent.KeyMaterial
import io.github.aakira.napier.Napier
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Fork of the vclib `DefaultCryptoService`, but with biometric callbacks to authenticate the user
 *
 */
class AndroidCryptoService(
    override val keyMaterial: KeyMaterial
) : WalletCryptoService(keyMaterial) {

    override suspend fun sign(input: ByteArray) =
        if (keyMaterial.getUnderLyingSigner() is AndroidKeystoreSigner) { //TODO: we need a far better strategy to pass prompt messages
            Napier.d { "ANDROID KS with prompt title: $promptText and subtitle $promptSubtitle" }
            (keyMaterial.getUnderLyingSigner() as AndroidKeystoreSigner).sign(input) {
                unlockPrompt {
                    promptText?.let { message = it }
                    promptSubtitle?.let { subtitle = it }
                    promptCancelText?.let { cancelText = it }
                }
            }
        } else super.sign(input)


    override fun encrypt(
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray,
        input: ByteArray,
        algorithm: JweEncryption,
    ): KmmResult<AuthenticatedCiphertext> = try {
        val jcaCiphertext = Cipher.getInstance(algorithm.jcaName).also {
            it.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, algorithm.jcaKeySpecName),
                GCMParameterSpec(algorithm.ivLengthBits, iv)
            )
            it.updateAAD(aad)
        }.doFinal(input)
        val ciphertext = jcaCiphertext.dropLast(algorithm.ivLengthBits / 8).toByteArray()
        val authtag = jcaCiphertext.takeLast(algorithm.ivLengthBits / 8).toByteArray()
        KmmResult.success(AuthenticatedCiphertext(ciphertext, authtag))
    } catch (e: Throwable) {
        KmmResult.failure(e)
    }

    override suspend fun decrypt(
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray,
        input: ByteArray,
        authTag: ByteArray,
        algorithm: JweEncryption,
    ): KmmResult<ByteArray> = try {
        val plaintext = Cipher.getInstance(algorithm.jcaName, "AndroidKeyStore").also {
            it.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, algorithm.jcaKeySpecName),
                GCMParameterSpec(algorithm.ivLengthBits, iv)
            )
            it.updateAAD(aad)
        }.doFinal(input + authTag)
        KmmResult.success(plaintext)
    } catch (e: Throwable) {
        KmmResult.failure(e)
    }

}

