package preview.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.DrivingData
import ui.composables.PersonDrivingDataDetailCard

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonDrivingDataDetailCard(
            drivingData = DrivingData(
                drivingPermissions = listOf("Klasse A", "Klasse B")
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
