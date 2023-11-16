package at.asitplus.wallet.app.android

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.CryptoPublicKey
import at.asitplus.wallet.lib.agent.AuthenticatedCiphertext
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.Digest
import at.asitplus.wallet.lib.agent.EphemeralKeyHolder
import at.asitplus.wallet.lib.agent.JvmEphemeralKeyHolder
import at.asitplus.wallet.lib.agent.getPublicKey
import at.asitplus.wallet.lib.agent.jcaKeySpecName
import at.asitplus.wallet.lib.agent.jcaName
import at.asitplus.wallet.lib.cbor.CoseAlgorithm
import at.asitplus.wallet.lib.jws.EcCurve
import at.asitplus.wallet.lib.jws.EcCurve.SECP_256_R_1
import at.asitplus.wallet.lib.jws.JsonWebKey
import at.asitplus.wallet.lib.jws.JweAlgorithm
import at.asitplus.wallet.lib.jws.JweEncryption
import at.asitplus.wallet.lib.jws.JwsAlgorithm
import at.asitplus.wallet.lib.jws.JwsExtensions.ensureSize
import at.asitplus.wallet.lib.jws.MultibaseHelper
import java.security.KeyPair
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.Certificate
import java.security.interfaces.ECPublicKey
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Fork of the vclib `DefaultCryptoService`, but with biometric callbacks to authenticate the user
 *
 * TODO Implement biometric collbacks
 */
class AndroidCryptoService(private val keyPair: KeyPair, certificate: Certificate) :
    CryptoService {

    private val ecCurve: EcCurve = SECP_256_R_1
    private val cryptoPublicKey: CryptoPublicKey
    override val certificate: ByteArray
    override val jwsAlgorithm = JwsAlgorithm.ES256
    override val coseAlgorithm = CoseAlgorithm.ES256

    override fun toPublicKey() = cryptoPublicKey

    override suspend fun sign(input: ByteArray): KmmResult<ByteArray> =
        try {
            val signed = Signature.getInstance(jwsAlgorithm.jcaName).apply {
                initSign(keyPair.private)
                update(input)
            }.sign()
            KmmResult.success(signed)
        } catch (e: Throwable) {
            KmmResult.failure(e)
        }

    override fun encrypt(
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray,
        input: ByteArray,
        algorithm: JweEncryption
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
        algorithm: JweEncryption
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

    override fun performKeyAgreement(
        ephemeralKey: EphemeralKeyHolder,
        recipientKey: JsonWebKey,
        algorithm: JweAlgorithm
    ): KmmResult<ByteArray> {
        require(ephemeralKey is JvmEphemeralKeyHolder) { "JVM Type expected" }
        return try {
            val secret = KeyAgreement.getInstance(algorithm.jcaName).also {
                it.init(ephemeralKey.keyPair.private)
                it.doPhase(recipientKey.getPublicKey(), true)
            }.generateSecret()
            KmmResult.success(secret)
        } catch (e: Throwable) {
            KmmResult.failure(e)
        }
    }

    override fun performKeyAgreement(ephemeralKey: JsonWebKey, algorithm: JweAlgorithm): KmmResult<ByteArray> =
        KmmResult.failure(NotImplementedError())

    override fun generateEphemeralKeyPair(ecCurve: EcCurve): KmmResult<EphemeralKeyHolder> =
        KmmResult.success(JvmEphemeralKeyHolder(ecCurve))

    override fun messageDigest(input: ByteArray, digest: Digest): KmmResult<ByteArray> = try {
        KmmResult.success(MessageDigest.getInstance(digest.jcaName).digest(input))
    } catch (e: Throwable) {
        KmmResult.failure(e)
    }

    init {
        val ecPublicKey = keyPair.public as ECPublicKey
        val keyX = ecPublicKey.w.affineX.toByteArray().ensureSize(ecCurve.coordinateLengthBytes)
        val keyY = ecPublicKey.w.affineY.toByteArray().ensureSize(ecCurve.coordinateLengthBytes)
        val keyId = MultibaseHelper.calcKeyId(SECP_256_R_1, keyX, keyY)!!
        this.cryptoPublicKey = CryptoPublicKey.Ec(curve = SECP_256_R_1, keyId = keyId, x = keyX, y = keyY)
        this.certificate = certificate.encoded
    }

}
