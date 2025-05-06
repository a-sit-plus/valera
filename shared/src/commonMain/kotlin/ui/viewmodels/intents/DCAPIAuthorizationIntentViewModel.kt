package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import ui.navigation.routes.Route

class DCAPIAuthorizationIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {

    fun process() {
        val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIData()
        val consentPageBuilder =
            BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()

        consentPageBuilder(dcApiRequest).unwrap().onSuccess {
            onSuccess(it)
        }.onFailure {
            onFailure(it)
        }
    }
}