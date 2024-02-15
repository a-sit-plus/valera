package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import view.OnboardingTermsScreen

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        OnboardingTermsScreen (
            onClickNavigateBack = {},
            onClickReadGeneralTermsAndConditions = {},
            onClickReadDataProtectionPolicy = {},
            onClickAccept = {},
        )
    }
}
