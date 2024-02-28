package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import view.ShowDataScreen

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        ShowDataScreen(
            navigateToAuthenticationStartPage = {},
            onClickShowDataToExecutive = {},
            onClickShowDataToOtherCitizen = {},
        )
    }
}
