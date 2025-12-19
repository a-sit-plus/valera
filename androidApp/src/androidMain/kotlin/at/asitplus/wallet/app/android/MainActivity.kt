package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.registry.provider.RegistryManager
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.PRESENTATION_REQUESTED_INTENT


class MainActivity : AbstractWalletActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val promptModel: PromptModel by lazy {
            AndroidPromptModel.Builder().apply { addCommonDialogs() }.build()
        }

        setContent {
            MainView(
                buildContext = BuildContext(
                    buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    osVersion = "Android ${Build.VERSION.RELEASE}"
                ),
                promptModel
            )
        }
    }

    override fun populateLink(intent: Intent) {
        when (intent.action) {
            RegistryManager.ACTION_GET_CREDENTIAL -> {
                Globals.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialResponseToDCAPIInvoker)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
            populateLink(intent)
    }
}