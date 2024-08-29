package at.asitplus.wallet.app.android

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG
import android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL
import android.security.keystore.KeyProperties.PURPOSE_SIGN
import android.security.keystore.KeyProperties.PURPOSE_VERIFY
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.dsl.REQUIRED
import at.asitplus.signum.supreme.os.AndroidKeyStoreProvider
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.HolderKeyService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.Date
import kotlin.time.Duration.Companion.seconds

interface KeyStoreService {
    suspend fun loadKeyPair(): KeyPair?
    fun loadCertificate(): Certificate?

    suspend fun getSigner(): Signer?

    fun clear()

}

class AndroidKeyStoreService : KeyStoreService, HolderKeyService {

    private val provider by lazy { AndroidKeyStoreProvider() }

    private val keyAlias = "binding"

    private lateinit var cert: X509Certificate

    override suspend fun getSigner(): Signer {

        Napier.d("getSigner")
        provider.getSignerForKey(keyAlias).onSuccess { return it }
        return provider.createSigningKey(alias = keyAlias) {
            hardware {
                protection {
                    factors {
                        biometry = true
                    }
                    timeout = Configuration.USER_AUTHENTICATION_TIMEOUT_SECONDS.seconds
                }
            }

        }.getOrThrow()

    }

    override suspend fun loadKeyPair(): KeyPair? {
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
    }

    override fun clear() {
        runBlocking { provider.deleteSigningKey(keyAlias) } //TODO check result
    }
}
