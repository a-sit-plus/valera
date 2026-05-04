package at.asitplus.wallet.app.android

import SharingView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.credentials.registry.provider.RegistryManager
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionService
import io.github.aakira.napier.Napier
import org.koin.core.context.GlobalContext
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT

class SharingActivity : AbstractWalletActivity() {
    private val intentState = IntentState()
    private val buildContext by lazy { createBuildContext() }
    private val promptModel: PromptModel by lazy {
        AndroidPromptModel.Builder().apply { addCommonDialogs() }.build()
    }
    private lateinit var sessionService: SessionService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.finishApp = { finish() }

        if (!::sessionService.isInitialized) {
            sessionService = SessionService().apply {
                initialize {
                    createWalletSessionScope(
                        koin = GlobalContext.get(),
                        sessionName = "sharing",
                        activity = this@SharingActivity,
                        intentState = intentState,
                        sessionService = this,
                        buildContext = buildContext,
                        promptModel = promptModel
                    )
                }
            }
        }

        setContent {
            SharingView(
                buildContext = buildContext,
                promptModel = promptModel,
                intentState = intentState,
                sessionService = sessionService
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

    override fun onDestroy() {
        if (::sessionService.isInitialized && isFinishing) {
            sessionService.close()
        }
        super.onDestroy()
    }
}
