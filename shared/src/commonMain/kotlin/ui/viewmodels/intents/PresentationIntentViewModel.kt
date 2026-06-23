package ui.viewmodels.intents

import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSource
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import io.github.aakira.napier.Napier
import ui.navigation.routes.Route

class PresentationIntentViewModel(
    val walletMain: WalletMain,
    private val localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator,
    val intentState: IntentState,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    suspend fun process() {
        val consentPageBuilder =
            BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment()

        Napier.d(
            "PresentationIntentViewModel.process uri=$uri initialModel=" +
                    "${intentState.presentationStateModel.value != null} " +
                    "hash=${intentState.presentationStateModel.value?.hashCode()}"
        )
        val activeSession = localPresentmentSessionCoordinator.activeSession()
            ?.takeIf { it.source == LocalPresentmentSource.ANDROID_EXTERNAL_NFC }
        val presentationStateModel = activeSession?.presentationStateModel
        Napier.d(
            "PresentationIntentViewModel.process resolvedModel=" +
                    "${presentationStateModel != null} hash=${presentationStateModel?.hashCode()}"
        )
        if (activeSession != null) {
            localPresentmentSessionCoordinator.markUiAttached(activeSession.sessionId)
            intentState.presentationStateModel.value = presentationStateModel
        }

        consentPageBuilder(
            presentationStateModel
        ).unwrap()
            .onSuccess {
                onSuccess(it)
            }.onFailure {
                onFailure(it)
            }
    }
}
