package preview.ui.views

import ui.views.AuthenticationSPInfoView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationSPInfoView(
            navigateUp = {},
            cancelAuthentication = {},
            authenticateAtSp = {},
            spName = "Post-Schalter#3",
            spLocation = "St. Peter Hauptstraße\n8010, Graz"
        )
    }
}
