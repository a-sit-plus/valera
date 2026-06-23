package ui.viewmodels.intents

import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.SigningState
import at.asitplus.wallet.app.common.WalletMain

class SigningResumeIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onReturnToSigning: () -> Unit,
    val onFinish: () -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    suspend fun process() {
        when (walletMain.signingService.state) {
            SigningState.PreloadCredential -> handle(
                action = { walletMain.signingService.resumePreloadCertificate(uri) },
                onSuccess = onReturnToSigning
            )

            SigningState.ServiceRequest -> handle(
                action = { walletMain.signingService.resumeWithServiceAuthCode(uri) },
                onSuccess = onReturnToSigning
            )

            SigningState.CredentialRequest -> handle(
                action = { walletMain.signingService.resumeWithCredentialAuthCode(uri) },
                onSuccess = onFinish
            )

            null -> onFailure(IllegalStateException("Signing callback received without an active signing flow"))
        }
    }

    private suspend fun handle(action: suspend () -> Unit, onSuccess: () -> Unit) {
        catchingUnwrapped {
            action()
        }.onSuccess {
            onSuccess()
        }.onFailure {
            walletMain.signingService.state = null
            onFailure(it)
        }
    }
}
