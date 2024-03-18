package at.asitplus.wallet.app.android

import at.asitplus.KmmResult
import at.asitplus.crypto.datatypes.CryptoAlgorithm
import at.asitplus.crypto.datatypes.CryptoPublicKey
import at.asitplus.crypto.datatypes.CryptoSignature
import at.asitplus.crypto.datatypes.Digest
import at.asitplus.crypto.datatypes.EcCurve
import at.asitplus.crypto.datatypes.asn1.ensureSize
import at.asitplus.crypto.datatypes.cose.CoseAlgorithm
import at.asitplus.crypto.datatypes.cose.CoseKey
import at.asitplus.crypto.datatypes.cose.toCoseKey
import at.asitplus.crypto.datatypes.fromJcaPublicKey
import at.asitplus.crypto.datatypes.getJcaPublicKey
import at.asitplus.crypto.datatypes.jcaName
import at.asitplus.crypto.datatypes.jws.JsonWebKey
import at.asitplus.crypto.datatypes.jws.JweAlgorithm
import at.asitplus.crypto.datatypes.jws.JweEncryption
import at.asitplus.crypto.datatypes.jws.JwsAlgorithm
import at.asitplus.crypto.datatypes.jws.jcaKeySpecName
import at.asitplus.crypto.datatypes.jws.jcaName
import at.asitplus.crypto.datatypes.jws.toJsonWebKey
import at.asitplus.crypto.datatypes.pki.X509Certificate
import at.asitplus.wallet.lib.agent.AuthenticatedCiphertext
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.EphemeralKeyHolder
import at.asitplus.wallet.lib.agent.JvmEphemeralKeyHolder
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

    private val ecCurve: EcCurve = EcCurve.SECP_256_R_1
    private val cryptoPublicKey: CryptoPublicKey
    override val certificate: X509Certificate
    override val coseKey: CoseKey
        get() = publicKey.toCoseKey().getOrThrow()
    override val jsonWebKey: JsonWebKey
        get() = publicKey.toJsonWebKey()
    override val algorithm: CryptoAlgorithm = CryptoAlgorithm.ES256
    override val publicKey: CryptoPublicKey get() = cryptoPublicKey

    override suspend fun sign(input: ByteArray): KmmResult<CryptoSignature> =
        try {
            val signed = Signature.getInstance(algorithm.jcaName).apply {
                initSign(keyPair.private)
                update(input)
            }.sign()

            KmmResult.success(CryptoSignature.EC(signed))
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
                it.doPhase(
                    recipientKey.toCryptoPublicKey().getOrThrow().getJcaPublicKey().getOrThrow(),
                    true
                )
            }.generateSecret()
            KmmResult.success(secret)
        } catch (e: Throwable) {
            KmmResult.failure(e)
        }
    }

    override fun performKeyAgreement(
        ephemeralKey: JsonWebKey,
        algorithm: JweAlgorithm
    ): KmmResult<ByteArray> =
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
        val pubKey = CryptoPublicKey.fromJcaPublicKey(ecPublicKey).getOrThrow()
        val keyId = pubKey.didEncoded
        this.cryptoPublicKey =
            pubKey
        this.certificate = X509Certificate.decodeFromDer(certificate.encoded)
    }

}
