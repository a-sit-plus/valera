package ui.viewmodels.intents

import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_title
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import ui.navigation.routes.Route
import ui.navigation.routes.TransientFlowIssuingResultRoute
import org.jetbrains.compose.resources.getString

class ProvisioningIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route?) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    suspend fun process() {
        catchingUnwrapped {
            walletMain.keyMaterial.promptText =
                getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
            val storedEntryIds = walletMain.provisioningService.resumeWithAuthCode(
                redirectedUrl = uri,
                statusUpdater = { storeId, status ->
                    walletMain.credentialValidityService.updateStatus(storeId, status)
                }
            )
            Napier.d("ProvisioningIntentViewModel success storedEntryIds=$storedEntryIds")
            TransientFlowIssuingResultRoute(storedEntryIds.firstOrNull())
        }.onSuccess {
            Napier.d("ProvisioningIntentViewModel navigating to route=$it")
            onSuccess(it)
        }.onFailure { error ->
            onFailure(error)
        }
    }
}
