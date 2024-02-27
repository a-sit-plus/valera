package preview.ui.views

import view.ShowDataScreen
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        ShowDataScreen(
            navigateToAthenticationStartPage = {},
            onClickShowDataToExecutive = {},
            onClickShowDataToOtherCitizen = {},
        )
    }
}
