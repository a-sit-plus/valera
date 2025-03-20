package data.storage

import android.content.Context
import android.content.SharedPreferences
import com.android.identity.crypto.X509Cert

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
}