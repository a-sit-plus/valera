package view

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.WalletMain


@Composable
fun QrCodeCredentialScannerScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    CameraScreen(
        title = "Credential Scanner",
        navigateUp = navigateUp,
        onPayloadFound = { payload ->
            walletMain.snackbarService.showSnackbar("Incomplete Implementation")
            navigateUp()
        }
    )
}