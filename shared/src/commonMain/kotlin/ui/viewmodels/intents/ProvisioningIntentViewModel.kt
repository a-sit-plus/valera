package ui.viewmodels.intents

import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_title
import at.asitplus.valera.resources.snackbar_credential_loaded_successfully
import at.asitplus.wallet.app.common.WalletMain
import org.jetbrains.compose.resources.getString

class ProvisioningIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: () -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    suspend fun process() {
        catchingUnwrapped {
            walletMain.keyMaterial.promptText =
                getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
            walletMain.provisioningService.resumeWithAuthCode(uri)
            walletMain.snackbarService.showSnackbar(getString(Res.string.snackbar_credential_loaded_successfully))
        }.onSuccess {
            onSuccess()
        }.onFailure { error ->
            onFailure(error)
        }
    }
}