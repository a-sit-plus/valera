package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import view.AuthenticationQrCodeScannerScreen

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationQrCodeScannerScreen(
            navigateUp = {},
            onPayloadFound = { payload -> }
        )
    }
}
