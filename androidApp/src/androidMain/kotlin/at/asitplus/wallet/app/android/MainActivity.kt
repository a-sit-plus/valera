package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.registry.provider.RegistryManager
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import io.github.aakira.napier.Napier
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT


class MainActivity : AbstractWalletActivity() {
    private val intentState = IntentState()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intentState.finishApp = { finish() }

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
                promptModel,
                intentState = intentState
            )
        }
    }

    override fun populateLink(intent: Intent) {
        Napier.d("MainActivity.populateLink url=${intent.data} action=${intent.action}")
        when (intent.action) {
            RegistryManager.ACTION_GET_CREDENTIAL -> {
                Napier.d("MainActivity DCAPI GET_CREDENTIAL")
                intentState.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialResponseToDCAPIInvoker)
                intentState.appLink.value = intent.action
            }
            RegistryManager.ACTION_CREATE_CREDENTIAL -> {
                Napier.d("MainActivity DCAPI CREATE_CREDENTIAL")
                intentState.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialCreationResponseToDCAPIInvoker)
                intentState.appLink.value = intent.action
            }
            PRESENTATION_REQUESTED_INTENT -> {
                Napier.d("MainActivity PRESENTATION_REQUESTED_INTENT")
                intentState.presentationStateModel.value = NdefDeviceEngagementService.presentationStateModel
                intentState.appLink.value = PRESENTATION_REQUESTED_INTENT
            }
            else -> {
                Napier.d("MainActivity appLink=${intent.data}")
                intentState.appLink.value = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        populateLink(intent)
    }
}
