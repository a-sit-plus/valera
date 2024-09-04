package at.asitplus.wallet.app.common

import at.asitplus.io.MultiBase
import at.asitplus.io.multibaseDecode
import at.asitplus.io.multibaseEncode
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.SigningProvider
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.lib.agent.KeyWithCert
import at.asitplus.wallet.lib.agent.KeyWithSelfSignedCert
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds


class KeystoreService(
    private val dataStoreService: DataStoreService
) : HolderKeyService {

    suspend fun getSigner(): KeyWithCert {

        val sMut = Mutex()
        var signer: KeyWithCert? = null

        Napier.d("getSigner")

        sMut.withLock {
            if (signer == null)
                signer = initSigner()
        }
        return signer!!
    }

    private suspend fun initSigner(): KeyWithSelfSignedCert {
        getProvider().let { provider ->
            val forKey = provider.getSignerForKey(Configuration.KS_ALIAS)
            forKey.onSuccess {
                return object : KeyWithSelfSignedCert(listOf()), Signer by it {
                    override suspend fun getCertificate(): X509Certificate =
                        dataStoreService.getPreference(
                            "MB64_USER_CERT"
                        ).map { it!!.multibaseDecode() }
                            .map { X509Certificate.decodeFromDer(it!!) }
                            .first().also { println(it.encodeToTlv().prettyPrint()) }
                }


            }.onFailure {
                return provider.createSigningKey(alias = Configuration.KS_ALIAS) {
                    hardware {
                        protection {
                            factors {
                                biometry = true
                            }
                            timeout = Configuration.USER_AUTHENTICATION_TIMEOUT_SECONDS.seconds
                        }
                    }
                }.getOrThrow().let {
                    //TODO this is really problematic
                    object : KeyWithSelfSignedCert(listOf()), Signer by it {

                        private var crt: X509Certificate? = null
                        override suspend fun getCertificate(): X509Certificate {
                            if (crt != null) return crt!!
                            return super.getCertificate().also {
                                crt = it
                                Napier.e { "STORE CERT" }
                                dataStoreService.setPreference(
                                    it.encodeToDer().multibaseEncode(MultiBase.Base.BASE64),
                                    "MB64_USER_CERT"
                                )
                            }
                        }
                    }
                }
            }
        }

        throw IllegalStateException("HOW?")
    }


    //TMP for iOS
    fun getSignerBlocking() = runBlocking { getSigner() }

    override fun clear() {
        runBlocking { getProvider().deleteSigningKey(Configuration.KS_ALIAS) } //TODO check result
    }
}

expect fun getProvider(): SigningProvider