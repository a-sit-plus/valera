package previews

import ui.views.AuthenticationQrCodeScannerView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationQrCodeScannerView(
            navigateUp = {},
            navigateToConsentScreenWithResult = { name, location -> }
        )
    }
}
