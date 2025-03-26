package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import at.asitplus.wallet.app.android.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
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

    override fun populateLink(intent: Intent) {
        when (intent.action) {
            IntentHelper.ACTION_GET_CREDENTIAL -> {
                Globals.dcapiInvocationData.value = DCAPIInvocationData(intent)
                Globals.appLink.value = intent.action
            }
            PRESENTATION_REQUESTED_INTENT -> {
                Globals.presentationStateModel.value = NdefDeviceEngagementService.presentationStateModel
                Globals.appLink.value = PRESENTATION_REQUESTED_INTENT
            }
            else -> {
                Globals.appLink.value = intent.data?.toString()
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