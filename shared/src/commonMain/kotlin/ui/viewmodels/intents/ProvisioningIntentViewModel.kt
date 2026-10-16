package ui.viewmodels.intents

import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import ui.navigation.routes.CredentialReviewRoute
import ui.navigation.routes.Route
import ui.navigation.routes.TransientFlowIssuingResultRoute

class ProvisioningIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route?) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    suspend fun process() {
        catchingUnwrapped {
            val stored = walletMain.provisioningService.resumeWithAuthCode(
                redirectedUrl = uri,
                statusUpdater = { storeId, status ->
                    walletMain.credentialValidityService.updateStatus(storeId, status)
                },
                onProgress = walletMain.loadingStatusService::set,
            )
            walletMain.loadingStatusService.clear()
            Napier.d("ProvisioningIntentViewModel success $stored")
            if (stored.needsReview) CredentialReviewRoute
            else TransientFlowIssuingResultRoute(stored.storedEntryIds.firstOrNull())
        }.onSuccess {
            Napier.d("ProvisioningIntentViewModel navigating to route=$it")
            onSuccess(it)
        }.onFailure { error ->
            walletMain.loadingStatusService.clear()
            onFailure(error)
        }
    }
}
