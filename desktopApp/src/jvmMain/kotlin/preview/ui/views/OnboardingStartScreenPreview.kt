package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import view.OnboardingStartScreen

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        OnboardingStartScreen(
            onClickStart = {},
        )
    }
}
