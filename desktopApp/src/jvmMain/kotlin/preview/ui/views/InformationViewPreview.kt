package preview.ui.views

import ui.views.InformationView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        InformationView(
            stage = "T",
            version = "1.0.0 / 2389237",
            onClickFAQs = {},
            onClickDataProtectionPolicy = {},
            onClickLicenses = {},
            onClickShareLogFile = {},
            onClickResetApp = {},
        )
    }
}
