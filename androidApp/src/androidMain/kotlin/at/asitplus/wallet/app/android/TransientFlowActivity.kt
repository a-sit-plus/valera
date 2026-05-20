package at.asitplus.wallet.app.android

import TransientFlowView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.credentials.registry.provider.RegistryManager
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionService
import io.github.aakira.napier.Napier
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT

class TransientFlowActivity : AbstractWalletActivity() {
    private val intentState = IntentState()
    private val buildContext by lazy { createAndroidBuildContext() }
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
                    createAndroidTransientFlowWalletSessionScope(
                        sessionName = "transientFlow",
                        activity = this@TransientFlowActivity,
                        intentState = intentState,
                        sessionService = this,
                        buildContext = buildContext,
                        promptModel = promptModel
                    )
                }
            }
        }

        setContent {
            TransientFlowView(
                buildContext = buildContext,
                promptModel = promptModel,
                intentState = intentState,
                sessionService = sessionService
            )
        }
    }

    override fun populateLink(intent: Intent) {
        Napier.d("TransientFlowActivity.populateLink url=${intent.data} action=${intent.action}")
        when (intent.action) {
            RegistryManager.ACTION_GET_CREDENTIAL -> {
                Napier.d("TransientFlowActivity DCAPI GET_CREDENTIAL")
                intentState.presentationStateModel.value = null
                intentState.presentationStateModelProvider = null
                intentState.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialResponseToDCAPIInvoker)
                intentState.appLink.value = intent.action
            }
            RegistryManager.ACTION_CREATE_CREDENTIAL -> {
                Napier.d("TransientFlowActivity DCAPI CREATE_CREDENTIAL")
                intentState.presentationStateModel.value = null
                intentState.presentationStateModelProvider = null
                intentState.dcapiInvocationData.value =
                    AndroidDCAPIInvocationData(intent, ::sendCredentialCreationResponseToDCAPIInvoker)
                intentState.appLink.value = intent.action
            }
            PRESENTATION_REQUESTED_INTENT -> {
                Napier.d("TransientFlowActivity PRESENTATION_REQUESTED_INTENT")
                intentState.dcapiInvocationData.value = null
                intentState.presentationStateModelProvider = {
                    NdefDeviceEngagementService.currentPresentationStateModel
                }
                val model = NdefDeviceEngagementService.currentPresentationStateModel
                Napier.d(
                    "TransientFlowActivity currentPresentationStateModel=${model != null} " +
                            "hash=${model?.hashCode()}"
                )
                intentState.presentationStateModel.value = model
                Napier.d(
                    "TransientFlowActivity intentState.presentationStateModel=" +
                            "${intentState.presentationStateModel.value != null} " +
                            "hash=${intentState.presentationStateModel.value?.hashCode()}"
                )
                intentState.appLink.value = PRESENTATION_REQUESTED_INTENT
            }
            else -> {
                Napier.d("TransientFlowActivity appLink=${intent.data}")
                intentState.dcapiInvocationData.value = null
                intentState.presentationStateModel.value = null
                intentState.presentationStateModelProvider = null
                intentState.appLink.value = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        populateLink(intent)
    }

    override fun onDestroy() {
        // Guard against configuration changes: onDestroy is also called on rotation,
        // locale switches, etc. without isFinishing = true. Closing the session there cancels
        // in-flight coroutines and leaks the old Koin scope while the new Activity instance is
        // already initialising a replacement session.
        if (isFinishing && ::sessionService.isInitialized) {
            sessionService.close()
        }
        super.onDestroy()
    }
}
