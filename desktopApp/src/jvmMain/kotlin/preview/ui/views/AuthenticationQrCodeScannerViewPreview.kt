package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.views.AuthenticationQrCodeScannerView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationQrCodeScannerView(
            navigateUp = {},
            onPayloadFound = { payload -> }
        )
    }
}
