package at.asitplus.wallet.app.android

import at.asitplus.KmmResult
import at.asitplus.KmmResult.Companion.wrap
import at.asitplus.signum.indispensable.getJcaPublicKey
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JweAlgorithm
import at.asitplus.signum.indispensable.josef.JweEncryption
import at.asitplus.signum.indispensable.josef.isAuthenticatedEncryption
import at.asitplus.signum.indispensable.josef.jcaHmacName
import at.asitplus.signum.indispensable.josef.jcaKeySpecName
import at.asitplus.signum.indispensable.josef.jcaName
import at.asitplus.signum.supreme.HazardousMaterials
import at.asitplus.signum.supreme.hazmat.jcaPrivateKey
import at.asitplus.signum.supreme.os.AndroidKeystoreSigner
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.lib.agent.AuthenticatedCiphertext
import at.asitplus.wallet.lib.agent.EphemeralKeyHolder
import at.asitplus.wallet.lib.agent.KeyMaterial
import io.github.aakira.napier.Napier
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
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
    ): KmmResult<AuthenticatedCiphertext> = runCatching {
        val jcaCiphertext = Cipher.getInstance(algorithm.jcaName).also {
            it.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, algorithm.jcaKeySpecName),
                IvParameterSpec(iv)
            )
            if (algorithm.isAuthenticatedEncryption) {
                it.updateAAD(aad)
            }
        }.doFinal(input)
        if (algorithm.isAuthenticatedEncryption) {
            val ciphertext = jcaCiphertext.dropLast(algorithm.ivLengthBits / 8).toByteArray()
            val authtag = jcaCiphertext.takeLast(algorithm.ivLengthBits / 8).toByteArray()
            AuthenticatedCiphertext(ciphertext, authtag)
        } else {
            AuthenticatedCiphertext(jcaCiphertext, byteArrayOf())
        }
    }.wrap()

    override suspend fun decrypt(
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray,
        input: ByteArray,
        authTag: ByteArray,
        algorithm: JweEncryption,
    ): KmmResult<ByteArray> = runCatching {
        val wholeInput = input + if (algorithm.isAuthenticatedEncryption) authTag else byteArrayOf()
        Cipher.getInstance(algorithm.jcaName).also {
            it.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, algorithm.jcaKeySpecName),
                IvParameterSpec(iv)
            )
            if (algorithm.isAuthenticatedEncryption) {
                it.updateAAD(aad)
            }
        }.doFinal(wholeInput)
    }.wrap()

    override fun hmac(
        key: ByteArray,
        algorithm: JweEncryption,
        input: ByteArray,
    ): KmmResult<ByteArray> = runCatching {
        Mac.getInstance(algorithm.jcaHmacName).also {
            it.init(SecretKeySpec(key, algorithm.jcaKeySpecName))
        }.doFinal(input)
    }.wrap()

    override fun performKeyAgreement(
        ephemeralKey: EphemeralKeyHolder,
        recipientKey: JsonWebKey,
        algorithm: JweAlgorithm
    ): KmmResult<ByteArray> = runCatching {
        val jvmKey = recipientKey.toCryptoPublicKey().getOrThrow().getJcaPublicKey().getOrThrow()
        KeyAgreement.getInstance(algorithm.jcaName).also {
            @OptIn(HazardousMaterials::class)
            it.init(ephemeralKey.key.jcaPrivateKey)
            it.doPhase(jvmKey, true)
        }.generateSecret()
    }.wrap()

    override fun performKeyAgreement(
        ephemeralKey: JsonWebKey,
        algorithm: JweAlgorithm
    ): KmmResult<ByteArray> = runCatching {
        val publicKey = ephemeralKey.toCryptoPublicKey().getOrThrow().getJcaPublicKey().getOrThrow()
        KeyAgreement.getInstance(algorithm.jcaName).also {
            @OptIn(HazardousMaterials::class)
            it.init(keyMaterial.getUnderLyingSigner().jcaPrivateKey)
            it.doPhase(publicKey, true)
        }.generateSecret()
    }.wrap()
}

