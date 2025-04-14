package at.asitplus.wallet.app.android

import App
import MainView
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import appLink
import at.asitplus.wallet.app.android.dcapi.WalletAPIData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import com.google.android.gms.identitycredentials.GetCredentialResponse
import com.google.android.gms.identitycredentials.IntentHelper
/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
            StatusBar()
        }
    }
}

@Composable
fun StatusBar() {
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f).toArgb()
    val darkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = primaryColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
*/

class MainActivity : AppCompatActivity() {

    private val walletAPIData = WalletAPIData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        populateLink(intent)
        setContent {
            MainView(
                buildContext = BuildContext(
                    buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    osVersion = "Android ${Build.VERSION.RELEASE}"
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
