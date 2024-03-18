package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.screens.AuthenticationSuccessView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationSuccessView(
            navigateUp = {},
        )
    }
}
