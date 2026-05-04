package at.asitplus.wallet.app.android

import SharingView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.credentials.registry.provider.RegistryManager
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import io.github.aakira.napier.Napier
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT

class SharingActivity : AbstractWalletActivity() {
    private val intentState = SharingIntentStateHolder.intentState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.finishApp = { finish() }

        val promptModel: PromptModel by lazy {
            AndroidPromptModel.Builder().apply { addCommonDialogs() }.build()
        }

        setContent {
            SharingView(
                buildContext = BuildContext(
                    buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    osVersion = "Android ${Build.VERSION.RELEASE}"
                ),
                promptModel = promptModel,
                intentState = intentState
            )
        }
    }

    override fun populateLink(intent: Intent) {
        Napier.d("SharingActivity.populateLink url=${intent.data} action=${intent.action}")
        when (intent.action) {
            RegistryManager.ACTION_GET_CREDENTIAL -> {
                Napier.d("SharingActivity DCAPI GET_CREDENTIAL")
                intentState.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialResponseToDCAPIInvoker)
                intentState.appLink.value = intent.action
            }
            RegistryManager.ACTION_CREATE_CREDENTIAL -> {
                Napier.d("SharingActivity DCAPI CREATE_CREDENTIAL")
                intentState.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialCreationResponseToDCAPIInvoker)
                intentState.appLink.value = intent.action
            }
            PRESENTATION_REQUESTED_INTENT -> {
                Napier.d("SharingActivity PRESENTATION_REQUESTED_INTENT")
                intentState.presentationStateModel.value = NdefDeviceEngagementService.presentationStateModel
                intentState.appLink.value = PRESENTATION_REQUESTED_INTENT
            }
            else -> {
                Napier.d("SharingActivity appLink=${intent.data}")
                intentState.appLink.value = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        populateLink(intent)
    }
}