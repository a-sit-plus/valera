package at.asitplus.wallet.app.android

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import at.asitplus.KmmResult
import at.asitplus.KmmResult.Companion.wrap
import at.asitplus.catching
import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.CryptoPublicKey.EC.Companion.fromUncompressed
import at.asitplus.signum.indispensable.CryptoSignature
import at.asitplus.signum.indispensable.Digest
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.asn1.ensureSize
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.cosef.toCoseAlgorithm
import at.asitplus.signum.indispensable.cosef.toCoseKey
import at.asitplus.signum.indispensable.getJCASignatureInstance
import at.asitplus.signum.indispensable.getJcaPublicKey
import at.asitplus.signum.indispensable.jcaName
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JweAlgorithm
import at.asitplus.signum.indispensable.josef.JweEncryption
import at.asitplus.signum.indispensable.josef.jcaKeySpecName
import at.asitplus.signum.indispensable.josef.jcaName
import at.asitplus.signum.indispensable.josef.jwkId
import at.asitplus.signum.indispensable.josef.toJsonWebKey
import at.asitplus.signum.indispensable.parseFromJca
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.AndroidCryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.lib.agent.AuthenticatedCiphertext
import at.asitplus.wallet.lib.agent.EphemeralKeyHolder
import at.asitplus.wallet.lib.agent.JvmEphemeralKeyHolder
import at.asitplus.wallet.lib.agent.KeyPairAdapter
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyPair
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * Fork of the vclib `DefaultCryptoService`, but with biometric callbacks to authenticate the user
 *
 * TODO Implement biometric collbacks
 */
class AndroidCryptoService(
    private val keyPair: KeyPair,
    val certificate: X509Certificate,
    val defaultAuthorizationPromptContext: AndroidCryptoServiceAuthorizationContext? = null,
) : WalletCryptoService() {
    private var authorizationPromptContext: AndroidCryptoServiceAuthorizationContext? = null
    private var authorizationResult: BiometricPrompt.AuthenticationResult? = null
    private var authorizationPromptMutex = Mutex()
    override suspend fun useAuthorizationContext(
        context: CryptoServiceAuthorizationContext, block: suspend () -> Unit
    ) = catching {
        authorizationPromptMutex.withLock {
            when (context) {
                is AndroidCryptoServiceAuthorizationContext -> {
                    authorizationPromptContext = context
                }

                else -> throw IllegalArgumentException("context")
            }
            block()
            authorizationPromptContext = null
            authorizationResult = null
        }
    }

    fun showBiometryPrompt(callback: BiometricPrompt.AuthenticationCallback) {
        authorizationResult?.let {
            callback.onAuthenticationSucceeded(it)
        } ?: CoroutineScope(Dispatchers.Main).launch {
            val authorizationContext =
                authorizationPromptContext ?: defaultAuthorizationPromptContext
                ?: throw IllegalStateException("Missing authorization prompt context")

            val context = authorizationContext.context
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                context as FragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        callback.onAuthenticationError(errorCode, errString)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        authorizationResult = result
                        callback.onAuthenticationSucceeded(result)
                    }

                    override fun onAuthenticationFailed() {
                        callback.onAuthenticationFailed()
                    }
                },
            )


            // TODO: the example code also differentiates whether a crypto object is used or not
            //  2024-08-05: acrusage: I think we always want to have a crypto object here though
            val signature = algorithm.getJCASignatureInstance().getOrThrow()
            signature.initSign(keyPair.private)
            Napier.d("opening biometry prompt")
            biometricPrompt.authenticate(
                authorizationContext.promptInfo,
                BiometricPrompt.CryptoObject(signature),
            )
        }
    }


    private val ecCurve: ECCurve = ECCurve.SECP_256_R_1
    private val cryptoPublicKey: CryptoPublicKey


    override val keyPairAdapter: KeyPairAdapter
        get() = object : KeyPairAdapter {
            override val certificate: X509Certificate
                get() = this@AndroidCryptoService.certificate

            override val coseKey: CoseKey
                get() = this@AndroidCryptoService.coseKey
            override val identifier: String
                get() = cryptoPublicKey.didEncoded
            override val jsonWebKey: JsonWebKey
                get() = this@AndroidCryptoService.jsonWebKey
            override val publicKey: CryptoPublicKey
                get() = this@AndroidCryptoService.publicKey
            override val signingAlgorithm: X509SignatureAlgorithm
                get() = this@AndroidCryptoService.algorithm
        }

    val algorithm: X509SignatureAlgorithm
        get() = X509SignatureAlgorithm.ES256

    val coseKey: CoseKey
        get() = publicKey.toCoseKey(algorithm.toCoseAlgorithm().getOrThrow()).getOrThrow()

    val jsonWebKey: JsonWebKey
        get() = publicKey.toJsonWebKey()

    val publicKey: CryptoPublicKey
        get() = cryptoPublicKey

    override suspend fun doSign(input: ByteArray): KmmResult<CryptoSignature> = runCatching {
        suspendCoroutine<CryptoSignature> { continuation ->
            showBiometryPrompt(
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        @BiometricPrompt.AuthenticationError errorCode: Int, errString: CharSequence
                    ) {
                        continuation.resumeWithException(Exception("$errorCode:$errString"))
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val signer = result.cryptoObject?.signature
                            ?: return continuation.resumeWithException(
                                IllegalStateException("Missing CryptoObject or Signature")
                            )

                        val signature = signer.run {
                            update(input)
                            sign()
                        }
                        continuation.resume(CryptoSignature.parseFromJca(signature, algorithm))
                    }

                    override fun onAuthenticationFailed() {
                        continuation.resumeWithException(Exception())
                    }
                },
            )
        }
    }.wrap()

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


    override fun performKeyAgreement(
        ephemeralKey: EphemeralKeyHolder,
        recipientKey: JsonWebKey,
        algorithm: JweAlgorithm,
    ): KmmResult<ByteArray> = runCatching {
        require(ephemeralKey is JvmEphemeralKeyHolder) { "JVM Type expected" }

        KeyAgreement.getInstance(algorithm.jcaName).also {
            it.init(ephemeralKey.keyPair.private)
            it.doPhase(recipientKey.toCryptoPublicKey().transform { it1 -> it1.getJcaPublicKey() }
                .getOrThrow(), true)
        }.generateSecret()
    }.wrap()

    override fun performKeyAgreement(
        ephemeralKey: JsonWebKey,
        algorithm: JweAlgorithm,
    ): KmmResult<ByteArray> = KmmResult.failure(NotImplementedError())

    override fun generateEphemeralKeyPair(ecCurve: ECCurve): KmmResult<EphemeralKeyHolder> =
        KmmResult.success(JvmEphemeralKeyHolder(ecCurve))

    override fun messageDigest(input: ByteArray, digest: Digest): KmmResult<ByteArray> = try {
        KmmResult.success(MessageDigest.getInstance(digest.jcaName).digest(input))
    } catch (e: Throwable) {
        KmmResult.failure(e)
    }

    init {
        val ecPublicKey = keyPair.public as ECPublicKey
        val keyX = ecPublicKey.w.affineX.toByteArray().ensureSize(ecCurve.coordinateLength.bytes)
        val keyY = ecPublicKey.w.affineY.toByteArray().ensureSize(ecCurve.coordinateLength.bytes)
        this.cryptoPublicKey =
            fromUncompressed(curve = ECCurve.SECP_256_R_1, x = keyX, y = keyY).also {
                    it.jwkId = it.didEncoded
                }
    }
}
