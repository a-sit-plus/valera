package previews

import ui.views.ShowDataView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        ShowDataView(
            navigateToAuthenticationAtSp = {},
            navigateToShowDataToExecutive = {},
            navigateToShowDataToOtherCitizen = {},
        )
    }
}
