package preview.ui.views

import ui.views.MyDataView
import ui.composables.IdentityData
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.datetime.LocalDate
import ui.composables.AgeData
import ui.composables.DrivingData

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MyDataView(
            addCredentials = {},
            identityData = IdentityData(
                name = "Max Mustermann",
                birthdate = LocalDate(year = 1990, monthNumber = 1, dayOfMonth = 1),
                picture = null,
            ),
            ageData = AgeData(
                ageLowerBounds = listOf(14, 16, 18),
                ageUpperBounds = listOf(21),
            ),
            drivingData = DrivingData(
                drivingPermissions = listOf("Klasse A", "Klasse B")
            ),
        )
    }
}
