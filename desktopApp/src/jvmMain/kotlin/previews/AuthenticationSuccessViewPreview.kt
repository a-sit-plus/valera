package previews

import ui.views.AuthenticationSuccessView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationSuccessView(
            navigateUp = {},
            completeAuthentication = {},
        )
    }
}
