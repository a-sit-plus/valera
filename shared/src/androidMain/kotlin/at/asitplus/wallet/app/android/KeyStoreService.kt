package at.asitplus.wallet.app.android

import Configuration
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG
import android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL
import android.security.keystore.KeyProperties.PURPOSE_SIGN
import android.security.keystore.KeyProperties.PURPOSE_VERIFY
import at.asitplus.wallet.app.common.HolderKeyService
import io.github.aakira.napier.Napier
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.Date

interface KeyStoreService {
    fun loadKeyPair(): KeyPair?
    fun loadCertificate(): Certificate?

    fun clear()

}

class AndroidKeyStoreService : KeyStoreService, HolderKeyService {

    private val keyAlias = "binding"

    override fun loadKeyPair(): KeyPair? {
        try {
            Napier.d("loadKeyPair")
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null, null) }
            val key = keyStore.getKey(keyAlias, null)
            val certificate = keyStore.getCertificate(keyAlias)
            if (key != null && key is PrivateKey && certificate != null) {
                return KeyPair(certificate.publicKey, key)
            }

            val builder = KeyGenParameterSpec.Builder(keyAlias, PURPOSE_SIGN or PURPOSE_VERIFY)
                .setKeySize(256)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setCertificateNotBefore(Date())
                .setUserAuthenticationRequired(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setUserAuthenticationParameters(Configuration.USER_AUTHENTICATION_TIMEOUT, AUTH_BIOMETRIC_STRONG or AUTH_DEVICE_CREDENTIAL)
                    }
                }
            return KeyPairGenerator.getInstance("EC", "AndroidKeyStore").apply {
                initialize(builder.build())
            }.generateKeyPair()
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

    override fun clear(){
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null, null) }
        keyStore.deleteEntry(keyAlias)
    }
}
