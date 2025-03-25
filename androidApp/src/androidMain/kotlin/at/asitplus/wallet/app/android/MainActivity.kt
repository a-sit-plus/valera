package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import at.asitplus.wallet.app.android.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import com.android.identity.util.AndroidContexts
import com.google.android.gms.identitycredentials.IntentHelper
import ui.navigation.PRESENTATION_REQUESTED_INTENT


class MainActivity : AbstractWalletActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainView(
                buildContext = BuildContext(
                    buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    osVersion = "Android ${Build.VERSION.RELEASE}"
                ),
                ::sendCredentialResponseToDCAPIInvoker
            )
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidContexts.setCurrentActivity(this)
    }

    override fun onPause() {
        super.onPause()
        AndroidContexts.setCurrentActivity(null)
    }

    override fun populateLink(intent: Intent) {
        when (intent.action) {
            IntentHelper.ACTION_GET_CREDENTIAL -> {
                GLOBALS.dcapiInvocationData.value = DCAPIInvocationData(intent)
                GLOBALS.appLink.value = intent.action
            }
            PRESENTATION_REQUESTED_INTENT -> {
                GLOBALS.presentationStateModel.value = NdefDeviceEngagementService.presentationStateModel
                GLOBALS.appLink.value = PRESENTATION_REQUESTED_INTENT
            }
            else -> {
                GLOBALS.appLink.value = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            populateLink(intent)
        }
    }
}