package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import ui.navigation.routes.Route

class AuthorizationIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit,
) {
    suspend fun process() {
        val consentPageBuilder = BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
            presentationService = walletMain.presentationService
        )
        consentPageBuilder(uri).unwrap().onSuccess {
            onSuccess(it)

        }.onFailure {
            onFailure(it)
        }
    }
}