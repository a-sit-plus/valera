package preview.view

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import view.ConsentScreen

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        ConsentScreen(
            navigateUp = {},
            onAccept = {},
            onCancel = {},
            claims = listOf("claim1", "claim2"),
            recipientName = "recipientName1",
            recipientLocation = "recipientLocation1",
        )
    }
}
