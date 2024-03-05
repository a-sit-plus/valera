package view

import Resources
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.WalletMain


@Composable
fun QrCodeCredentialScannerScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    CameraScreen(
        title = Resources.HEADING_LABEL_CREDENTIAL_SCANNER_SCREEN,
        navigateUp = navigateUp,
        onPayloadFound = { payload ->
            walletMain.snackbarService.showSnackbar(Resources.ERROR_FEATURE_NOT_YET_AVAILABLE)
            navigateUp()
        }
    )
}