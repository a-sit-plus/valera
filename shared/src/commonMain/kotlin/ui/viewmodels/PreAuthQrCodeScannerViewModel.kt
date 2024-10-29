package ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.CredentialOfferInfo
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

class PreAuthQrCodeScannerViewModel(
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val navigateToAddCredentialsPage: (CredentialOfferInfo) -> Unit
) {
    var isLoading by mutableStateOf(false)

    fun getCredential(payload: String) {
        Napier.d("onScan: $payload")

        walletMain.scope.launch {
            try {
                walletMain.provisioningService.decodeCredentialOffer(payload).let { offer ->
                    navigateToAddCredentialsPage(offer)
                }
            } catch (e: Throwable) {
                navigateUp()
                walletMain.errorService.emit(e)
            }
        }
    }
}