package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import appLink
import at.asitplus.wallet.app.common.BuildContext
import com.google.android.gms.identitycredentials.GetCredentialResponse
import com.google.android.gms.identitycredentials.IntentHelper
import at.asitplus.wallet.app.android.dcapi.WalletAPIData


class MainActivity : AppCompatActivity() {

    private val walletAPIData = WalletAPIData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        populateLink(intent)
        setContent {
            MainView(
                buildContext = BuildContext(
                    buildType = BuildConfig.BUILD_TYPE,
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                ),
                walletAPIData,
                ::sendCredentialResponseToDCAPIInvoker
            )
        }
    }

    private fun sendCredentialResponseToDCAPIInvoker(resultJson: String) {
        val resultData = Intent()
        val bundle = Bundle()
        bundle.putByteArray("identityToken", resultJson.toByteArray())
        val credentialResponse = com.google.android.gms.identitycredentials.Credential("type", bundle)

        IntentHelper.setGetCredentialResponse(
            resultData,
            GetCredentialResponse(credentialResponse)
        )
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun populateLink(intent: Intent) {
        walletAPIData.intent = intent
        appLink.value = if (intent.action == IntentHelper.ACTION_GET_CREDENTIAL) intent.action else intent.data?.toString()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            populateLink(intent)
        }
    }
}