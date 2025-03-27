package data.storage

import android.content.Context
import android.content.SharedPreferences
import com.android.identity.crypto.X509Cert
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.io.StringWriter
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object CertificateStorage {
    private const val PREF_NAME = "CertificateStorage"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    fun saveCertificate(context: Context, alias: String, pemCertificate: String) {
        val preferences = getPreferences(context)
        preferences.edit().putString(alias, pemCertificate).apply()
    }

    fun deleteCertificate(context: Context, alias: String) {
        val preferences = getPreferences(context)
        preferences.edit().remove(alias).apply()
    }

    fun loadCertificateAndroid(context: Context, alias: String): X509Cert? {
        val preferences = getPreferences(context)
        val pemCertificate = preferences.getString(alias, null) ?: return null
        return X509Cert.fromPem(pemCertificate)
    }

    fun pemToX509Certificate(pemData: String): X509Certificate? {
        val pemReader = PemReader(StringReader(pemData))
        return try {
            val pemObject = pemReader.readPemObject()
            val certificateData = pemObject.content
            val certificateFactory = CertificateFactory.getInstance("X.509")
            certificateFactory.generateCertificate(certificateData.inputStream()) as X509Certificate
        } catch (e: Exception) {
            null
        }
    }

    fun storeFromFileToPref(context: Context) {
        val pemCert = context.assets.open("SEAL.crt").bufferedReader().use { it.readText() }
        val cert = pemToX509Certificate(pemCert)
        saveCertificate(context, "SEAL", pemCert)
    }
}