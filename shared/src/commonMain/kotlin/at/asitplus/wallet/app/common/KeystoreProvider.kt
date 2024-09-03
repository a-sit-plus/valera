package at.asitplus.wallet.app.common

import at.asitplus.io.MultiBase
import at.asitplus.io.multibaseDecode
import at.asitplus.io.multibaseEncode
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.SigningProvider
import at.asitplus.signum.supreme.sign.SignatureInput
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.signum.supreme.wrap
import at.asitplus.wallet.lib.agent.DefaultKeyPairAdapter
import at.asitplus.wallet.lib.agent.generateSelfSignedCertificate
import data.storage.DataStoreService
import data.storage.RealDataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class SignerWithCert(signer: Signer,  val certificate: X509Certificate) : Signer by signer


class SignerKeyPairAdapter(signerWithCert: SignerWithCert) : DefaultKeyPairAdapter(
    signerWithCert,
    signerWithCert.certificate.tbsCertificate.extensions!!
) //TODO

class KeystoreService(private val dataStoreService: DataStoreService) : HolderKeyService {

    suspend fun getSigner(): SignerWithCert {

        Napier.d("getSigner")
        getProvider().let { provider ->
            provider.getSignerForKey(Configuration.KS_ALIAS).onSuccess {
                return it.run {
                    //TODO this is really problematic
                    SignerWithCert(this, dataStoreService.getPreference("MB64_USER_CERT").map { it!!.multibaseDecode() }.map { X509Certificate.decodeFromDer(it!!) }.single())
                }
            }
            return provider.createSigningKey(alias = Configuration.KS_ALIAS) {
                hardware {
                    protection {
                        factors {
                            biometry = true
                        }
                        timeout = Configuration.USER_AUTHENTICATION_TIMEOUT_SECONDS.seconds
                    }
                }

            }.getOrThrow().run { //TODO this is really problematic
                SignerWithCert(this,
                    X509Certificate.generateSelfSignedCertificate(
                        publicKey,
                        X509SignatureAlgorithm.ES256
                    ) { sign(SignatureInput(it)).wrap() /*TODO*/ }.also {
                        dataStoreService.setPreference(it.encodeToDer().multibaseEncode(MultiBase.Base.BASE64), "MB64_USER_CERT")
                    }
                )
            }
        }

    }

    //TMP for iOS
    fun getSignerBlocking() = runBlocking { getSigner() }


    /*  override suspend fun loadKeyPair(): KeyPair? {
          try {
              Napier.d("loadKeyPair")
              getSigner()
              val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null, null) }
              val key = keyStore.getKey(keyAlias, null)
              val certificate = keyStore.getCertificate(keyAlias)
              if (key != null && key is PrivateKey && certificate != null) {
                  return KeyPair(certificate.publicKey, key)
              }
              throw IllegalStateException("HOW?")
          } catch (e: Throwable) {
              Napier.e("loadKeyPair: error", e)
              return null
          }
      }

      override fun loadCertificate(): Certificate? {
          return try {
              Napier.d("loadCertificate")
              val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null, null) }
              keyStore.getCertificate(keyAlias)
          } catch (e: Throwable) {
              Napier.e("loadCertificate: error", e)
              null
          }
      }*/

    override fun clear() {
        runBlocking { getProvider().deleteSigningKey(Configuration.KS_ALIAS) } //TODO check result
    }
}

expect fun getProvider(): SigningProvider