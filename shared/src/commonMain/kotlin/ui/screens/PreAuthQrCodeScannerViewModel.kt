
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.launch

class PreAuthQrCodeScannerViewModel(val walletMain: WalletMain, val navigateUp: () -> Unit, val success: () -> Unit) {
    var isLoading by mutableStateOf(false)

    fun getCredential(payload: String) {
        walletMain.scope.launch {
            try {
                walletMain.provisioningService.decodeCredentialOffer(payload).let { offer ->
                    val credentialIssuer = offer.credentialOffer.credentialIssuer
                    val preAuthorizedCode =
                        offer.credentialOffer.grants?.preAuthorizedCode?.preAuthorizedCode.toString()
                    val credentialIdToRequest = offer.credentialOffer.configurationIds.first()

                    walletMain.provisioningService.loadCredentialWithPreAuthn(
                        credentialIssuer = credentialIssuer,
                        preAuthorizedCode = preAuthorizedCode,
                        credentialIdToRequest = credentialIdToRequest
                    )
                    success()
                }
            } catch (e: Throwable) {
                navigateUp()
                walletMain.errorService.emit(e)
            }
        }
    }
}