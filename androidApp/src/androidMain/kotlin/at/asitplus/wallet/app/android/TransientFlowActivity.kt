package at.asitplus.wallet.app.android

import TransientFlowView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.credentials.registry.provider.RegistryManager
import androidx.lifecycle.ViewModelProvider
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import io.github.aakira.napier.Napier
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT
import ui.navigation.IntentService.Companion.PROVISIONING_CALLBACK_URI

class TransientFlowActivity : AbstractWalletActivity() {
    private val sessionViewModel: AndroidWalletSessionViewModel by lazy {
        ViewModelProvider(
            this,
            AndroidWalletSessionViewModel.factory(
                applicationContext,
                AndroidWalletSessionKind.TRANSIENT_FLOW,
            ),
        )[AndroidWalletSessionViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionViewModel.attach(this)

        setContent {
            TransientFlowView(
                buildContext = sessionViewModel.buildContext,
                promptModel = sessionViewModel.promptModel,
                intentState = sessionViewModel.intentState,
                sessionService = sessionViewModel.sessionService,
            )
        }
    }

    override fun populateLink(intent: Intent) {
        if (!sessionViewModel.consumeInitialIntent()) return
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent) {
        Napier.d("TransientFlowActivity.populateLink url=${intent.data} action=${intent.action}")
        when (intent.action) {
            RegistryManager.ACTION_GET_CREDENTIAL -> {
                Napier.d("TransientFlowActivity DCAPI GET_CREDENTIAL")
                sessionViewModel.intentState.presentationStateModel.value = null
                sessionViewModel.intentState.presentationStateModelProvider = null
                sessionViewModel.intentState.dcapiInvocationData.value = AndroidDCAPIInvocationData(
                    intent,
                    sessionViewModel::sendCredentialResponseToInvoker,
                )
                sessionViewModel.intentState.appLink.value = intent.action
            }
            RegistryManager.ACTION_CREATE_CREDENTIAL -> {
                Napier.d("TransientFlowActivity DCAPI CREATE_CREDENTIAL")
                sessionViewModel.intentState.presentationStateModel.value = null
                sessionViewModel.intentState.presentationStateModelProvider = null
                sessionViewModel.intentState.dcapiInvocationData.value = AndroidDCAPIInvocationData(
                    intent,
                    sessionViewModel::sendCredentialCreationResponseToInvoker,
                )
                sessionViewModel.intentState.appLink.value = intent.action
            }
            PRESENTATION_REQUESTED_INTENT -> {
                Napier.d("TransientFlowActivity PRESENTATION_REQUESTED_INTENT")
                sessionViewModel.intentState.dcapiInvocationData.value = null
                sessionViewModel.intentState.presentationStateModelProvider = {
                    NdefDeviceEngagementService.currentPresentationStateModel
                }
                val model = NdefDeviceEngagementService.currentPresentationStateModel
                Napier.d(
                    "TransientFlowActivity currentPresentationStateModel=${model != null} " +
                            "hash=${model?.hashCode()}"
                )
                sessionViewModel.intentState.presentationStateModel.value = model
                Napier.d(
                    "TransientFlowActivity intentState.presentationStateModel=" +
                            "${sessionViewModel.intentState.presentationStateModel.value != null} " +
                            "hash=${sessionViewModel.intentState.presentationStateModel.value?.hashCode()}"
                )
                sessionViewModel.intentState.appLink.value = PRESENTATION_REQUESTED_INTENT
            }
            else -> {
                Napier.d("TransientFlowActivity appLink=${intent.data}")
                val preserveVerificationInvocation = intent.data?.toString()?.startsWith(PROVISIONING_CALLBACK_URI) == true &&
                    (sessionViewModel.intentState.dcapiInvocationData.value as? AndroidDCAPIInvocationData)
                        ?.intent
                        ?.action == RegistryManager.ACTION_GET_CREDENTIAL
                if (!preserveVerificationInvocation) {
                    sessionViewModel.intentState.dcapiInvocationData.value = null
                }
                sessionViewModel.intentState.presentationStateModel.value = null
                sessionViewModel.intentState.presentationStateModelProvider = null
                sessionViewModel.intentState.appLink.value = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntent(intent)
    }

    override fun onDestroy() {
        sessionViewModel.detach(this)
        super.onDestroy()
    }
}
