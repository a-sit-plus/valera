package at.asitplus.wallet.app.common

import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.SigningProvider
import at.asitplus.signum.supreme.sign.SignatureInput
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.lib.agent.generateSelfSignedCertificate
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class SignerWithCert(signer: Signer, val certificate: X509Certificate) : Signer by signer

class KeystoreService : HolderKeyService {

    suspend fun getSigner(): SignerWithCert {

        Napier.d("getSigner")
        getProvider().let { provider ->
            provider.getSignerForKey(Configuration.KS_ALIAS).onSuccess {
                return it.run {
                    SignerWithCert(this, X509Certificate.generateSelfSignedCertificate(
                        publicKey,
                        X509SignatureAlgorithm.ES256
                    ) { sign(SignatureInput(it)) })
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

            }.getOrThrow().run { //TODO STORE CERT SOMEWHERE
                SignerWithCert(this, X509Certificate.generateSelfSignedCertificate(
                    publicKey,
                    X509SignatureAlgorithm.ES256
                ) { sign(SignatureInput(it)) })
            }
        }

    }

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