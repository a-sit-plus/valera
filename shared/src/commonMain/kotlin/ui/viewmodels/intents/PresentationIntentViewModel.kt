package ui.viewmodels.intents

import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT
import ui.navigation.routes.Route

class PresentationIntentViewModel(
    val walletMain: WalletMain,
    val intentState: IntentState,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    fun process() {
        val consentPageBuilder =
            BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment()

        consentPageBuilder(
            PresentationRequest(PRESENTATION_REQUESTED_INTENT),
            intentState.presentationStateModel.value
        ).unwrap()
            .onSuccess {
                onSuccess(it)
            }.onFailure {
                onFailure(it)
            }
    }
}
