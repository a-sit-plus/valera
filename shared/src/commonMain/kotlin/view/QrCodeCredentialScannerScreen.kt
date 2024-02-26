package view

import androidx.compose.runtime.Composable


@Composable
fun QrCodeCredentialScannerScreen(
    navigateUp: () -> Unit,
    onPayloadFound: (String) -> Unit,
) {
    CameraScreen(
        title = "Credential Scanner",
        navigateUp = navigateUp,
        onPayloadFound = onPayloadFound,
    )
}