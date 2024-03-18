package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.screens.AuthenticationQrCodeScannerView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationQrCodeScannerView(
            navigateUp = { },
            onFoundPayload = { payload -> },
        )
    }
}
