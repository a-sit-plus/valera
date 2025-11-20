package at.asitplus.wallet.app.common

import AppResetRequiredException
import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.io.MultiBase
import at.asitplus.io.multibaseDecode
import at.asitplus.io.multibaseEncode
import at.asitplus.signum.indispensable.CryptoPrivateKey
import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.SecretExposure
import at.asitplus.signum.indispensable.SignatureAlgorithm
import at.asitplus.signum.indispensable.equalsCryptographically
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.SignatureResult
import at.asitplus.signum.supreme.dsl.PREFERRED
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.signum.supreme.sign.SignatureInput
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.KeyWithSelfSignedCert
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


private const val CERT_STORAGE_KEY = "MB64_CERT_SELF_SIGNED"

open class KeystoreService(
    private val dataStoreService: DataStoreService
) {
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    @Throws(Throwable::class)
    open suspend fun getSigner(): KeyMaterial = catchingUnwrapped { initSigner() }
        .getOrElse { FallBackKeyMaterial(it) }

    @Throws(Throwable::class)
    private suspend fun initSigner(alias: String): KeyWithSelfSignedCert = withContext(dispatcher) {
        PlatformSigningProvider.let { provider ->
            val forKey = provider.getSignerForKey(alias).getOrElse {
                provider.createSigningKey(alias = alias) {
                    ec {
                        curve = ECCurve.SECP_256_R_1
                        purposes {
                            keyAgreement = true
                            signing = true
                        }
                    }
                    hardware {
                        backing = PREFERRED
                        protection {
                            factors {
                                biometry = true
                            }
                            timeout = Configuration.BIOMETRIC_TIMEOUT
                        }
                    }
                }.getOrThrow()
            }
            KeyWithPersistentSelfSignedCert(forKey, forKey.publicKey.didEncoded, 30)
        }
    }

    @Throws(Throwable::class)
    private suspend fun initSigner(): KeyWithSelfSignedCert = initSigner(Configuration.KS_ALIAS)

    open suspend fun testSigner(): Boolean = withContext(dispatcher) {
        runCatching {
            PlatformSigningProvider.let { provider ->
                provider.deleteSigningKey(Configuration.KS_CAPABILITY_ALIAS)
                provider.createSigningKey(Configuration.KS_CAPABILITY_ALIAS)
            }
        }.isSuccess
    }

    suspend fun testAttestation() = withContext(dispatcher) {
        runCatching {
            PlatformSigningProvider.deleteSigningKey(Configuration.KS_CAPABILITY_ALIAS)
            PlatformSigningProvider.createSigningKey(Configuration.KS_CAPABILITY_ALIAS) {
                ec {}
                hardware {
                    attestation {
                        this.challenge = "CHALLENGE".encodeToByteArray()
                    }
                }
            }.getOrThrow()
        }.isSuccess
    }

    inner class KeyWithPersistentSelfSignedCert(
        private val signer: Signer,
        customKeyId: String,
        lifetimeInSeconds: Long
    ) : KeyWithSelfSignedCert(listOf(), customKeyId, lifetimeInSeconds), Signer by signer {
        override fun getUnderLyingSigner() = signer

        private val crtMut = Mutex()
        private var _certificate: X509Certificate? = null
        override suspend fun getCertificate(): X509Certificate? {
            crtMut.withLock {
                _certificate?.also { return it }
                repeat(3) {
                    (X509Certificate.load()?.let {
                        if (it.decodedPublicKey.getOrNull()?.equalsCryptographically(signer.publicKey) == true)
                            return it
                        else {
                            Napier.d { "Pre-stored Certificate mismatch. deleting!" }
                            dataStoreService.deletePreference(CERT_STORAGE_KEY)
                            null
                        }
                    }) ?: super.getCertificate()?.let { it.store(); _certificate = it; return it }
                }
            }
            Napier.w { "Could not load or generate self-signed certificate!" }
            return _certificate
        }

        private suspend fun X509Certificate.Companion.load(): X509Certificate? =
            dataStoreService.getPreference(
                CERT_STORAGE_KEY
            ).map { it?.multibaseDecode() }
                .map { it?.let { X509Certificate.decodeFromDer(it) } }
                .firstOrNull()
                .also { Napier.d { "Loaded certificate" + it?.encodeToTlv()?.prettyPrint() } }


        private suspend fun X509Certificate.store() {
            Napier.d { "Persistently storing certificate" }
            dataStoreService.setPreference(
                encodeToDer().multibaseEncode(MultiBase.Base.BASE64),
                CERT_STORAGE_KEY
            )
        }

    }


    companion object {
        @Throws(AppResetRequiredException::class)
        fun checkKeyMaterialValid() {
            PlatformSigningProvider.let { provider ->
                runBlocking {
                    provider.getSignerForKey(Configuration.KS_ALIAS_OLD).onSuccess {
                        provider.deleteSigningKey(Configuration.KS_ALIAS_OLD)
                            .getOrThrow() //well if we can't delete we're boned
                        throw AppResetRequiredException
                    }
                }
            }

        }

        fun clearKeyMaterial() {
            PlatformSigningProvider.let { provider ->
                runBlocking {
                    provider.getSignerForKey(Configuration.KS_ALIAS_OLD).onSuccess {
                        provider.deleteSigningKey(Configuration.KS_ALIAS_OLD)
                    }
                    provider.getSignerForKey(Configuration.KS_ALIAS).onSuccess {
                        provider.deleteSigningKey(Configuration.KS_ALIAS)
                    }
                }
            }
        }
    }

    //TMP for iOS
    @Throws(Throwable::class)
    fun getSignerBlocking() = runBlocking { getSigner() }
}

/**
 * Fallback class if KeyMaterial initialization throws on startup.
 * Allows to handle errors with the default navigation procedure.
 */
class FallBackKeyMaterial(
    val reason: Throwable,
    override val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.ECDSAwithSHA256,
    override val publicKey: CryptoPublicKey = EphemeralKeyWithoutCert().publicKey,
    override val identifier: String = ""
) : KeyMaterial {
    @SecretExposure
    override fun exportPrivateKey(): KmmResult<CryptoPrivateKey.WithPublicKey<*>> {
        throw NotImplementedError()
    }

    override suspend fun sign(data: SignatureInput): SignatureResult<*> {
        throw NotImplementedError()
    }

    override fun getUnderLyingSigner(): Signer {
        throw NotImplementedError()
    }

    override suspend fun getCertificate(): X509Certificate? {
        throw NotImplementedError()
    }

}
