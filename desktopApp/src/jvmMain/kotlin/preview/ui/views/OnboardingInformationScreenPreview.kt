package preview.ui.views

import view.OnboardingInformationScreen
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        OnboardingInformationScreen(
            onClickContinue = {},
        )
    }
}
