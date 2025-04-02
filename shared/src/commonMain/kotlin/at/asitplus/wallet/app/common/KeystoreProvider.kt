package at.asitplus.wallet.app.common

import at.asitplus.io.MultiBase
import at.asitplus.io.multibaseDecode
import at.asitplus.io.multibaseEncode
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.equalsCryptographically
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.dsl.PREFERRED
import at.asitplus.signum.supreme.os.SigningProvider
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.KeyWithSelfSignedCert
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds


private const val CERT_STORAGE_KEY = "MB64_CERT_SELF_SIGNED"

open class KeystoreService(
    private val dataStoreService: DataStoreService
) {
    private val sMut = Mutex()
    open suspend fun getSigner(): KeyMaterial {
        var signer: KeyMaterial? = null
        Napier.d("getSigner")
        sMut.withLock {
            if (signer == null)
                signer = initSigner()
        }
        return signer!!
    }

    private suspend fun initSigner(): KeyWithSelfSignedCert {
        getProvider().let { provider ->
            val existingKey: Signer.WithAlias? =
                provider.getSignerForKey(Configuration.KS_ALIAS).fold(
                    onSuccess = {
                        //TODO: how to let the user know that the key will be invalidated????
                        if (!it.mayRequireUserUnlock) {
                            Napier.w { "Your existing binding key will be invalidated and a new one will be created!" }
                            provider.deleteSigningKey(it.alias)
                                .onFailure { throw IllegalStateException("Could not delete outdated key!") }
                            null
                        } else it
                    },
                    onFailure = { null })

            val forKey = existingKey ?: provider.createSigningKey(alias = Configuration.KS_ALIAS) {
                ec { curve = ECCurve.SECP_256_R_1 }
                hardware {
                    backing =
                        PREFERRED //so it also works on the emulator!. In reality we would like REQUIRED!
                    protection {
                        factors { biometry = true }
                        timeout = Configuration.USER_AUTHENTICATION_TIMEOUT_SECONDS.seconds
                    }

                }
            }.getOrThrow()

            return KeyWithPersistentSelfSignedCert(forKey)
        }

    }


    inner class KeyWithPersistentSelfSignedCert(private val signer: Signer) :
        KeyWithSelfSignedCert(listOf()), Signer by signer {
        override fun getUnderLyingSigner() = signer


        private val crtMut = Mutex()
        private var _certificate: X509Certificate? = null
        override suspend fun getCertificate(): X509Certificate? {
            crtMut.withLock {
                _certificate?.also { return it }
                repeat(3) {
                    (X509Certificate.load()?.let {
                        if (it.publicKey.equalsCryptographically(signer.publicKey))
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


    //TMP for iOS
    fun getSignerBlocking() = runBlocking { getSigner() }
}

expect fun getProvider(): SigningProvider
