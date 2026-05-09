package ui.viewmodels.intents

import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment
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
            intentState.presentationStateModel.value
        ).unwrap()
            .onSuccess {
                onSuccess(it)
            }.onFailure {
                onFailure(it)
            }
    }
}
