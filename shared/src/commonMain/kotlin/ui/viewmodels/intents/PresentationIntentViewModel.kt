package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import ui.navigation.PRESENTATION_REQUESTED_INTENT
import ui.navigation.routes.Route

class PresentationIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    fun process() {
        val consentPageBuilder =
            BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment()

        consentPageBuilder(PresentationRequest(PRESENTATION_REQUESTED_INTENT)).unwrap()
            .onSuccess {
                onSuccess(it)
            }.onFailure {
                onFailure(it)
            }
    }
}