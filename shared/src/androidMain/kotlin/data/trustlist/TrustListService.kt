package data.trustlist

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.app.common.HttpService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import data.bletransfer.verifier.IdentityVerifier.fingerprintTrustList
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual fun getTrustListService(): TrustListService {
    return AndroidTrustListService()
}


class AndroidTrustListService: TrustListService {

    private lateinit var context: Context
    private val TAG: String = "TrustListService"
    private val PREF_NAME = "TrustlistStorage"
    private val TRUSTLIST_ALIAS = "trustlist"


    override fun setContext(context: Any) {
        this.context = context as Context
    }

    @Composable
    override fun setContext() {
        context = LocalContext.current
    }

    override fun fetchAndStoreTrustedFingerprints() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = HttpService()
                val client = service.buildHttpClient()
                val res = client.get("http://localhost:8080/api/certificate/seal/trustlist")
                fingerprintTrustList = parseCertificateChain(res)
                val preferences = getPreferences()
                preferences.edit()
                    .putString(TRUSTLIST_ALIAS, TextUtils.join(",", fingerprintTrustList)).apply()
            } catch (e: Exception) {
                Napier.e("Unable to fetch fingerprint trustlist.", e, TAG)
            }
        }
    }

    override fun getTrustedFingerprints(): List<String>? {
        val preferences = getPreferences()
        val savedTrustlist = preferences.getString(TRUSTLIST_ALIAS, null) ?: return null
        return savedTrustlist.split(",").toList()
    }

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private suspend fun parseCertificateChain(response: HttpResponse): List<String> {
        val gson = Gson()
        return gson.fromJson(
            response.bodyAsText(),
            object : TypeToken<List<String>>() {}.type
        )
    }
}