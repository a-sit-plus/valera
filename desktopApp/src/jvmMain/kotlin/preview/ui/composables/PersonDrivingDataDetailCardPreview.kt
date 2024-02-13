package preview.ui.composables

import ui.composables.DrivingData
import ui.composables.PersonDrivingDataDetailCard
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonDrivingDataDetailCard(
            drivingData = DrivingData(
                drivingPermissions = listOf("Klasse A", "Klasse B")
            ),
            onDetailClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
