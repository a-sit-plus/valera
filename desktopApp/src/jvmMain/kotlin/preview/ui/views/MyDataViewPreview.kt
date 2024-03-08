package preview.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.datetime.LocalDate
import ui.composables.AgeData
import ui.composables.DrivingData
import ui.composables.IdentityData
import ui.views.MyDataView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        Column {
            MyDataView(                identityData = IdentityData(
                    firstname = "Max",
                    lastname = "Mustermann",
                    dateOfBirth = LocalDate(year = 1990, monthNumber = 1, dayOfMonth = 1),
                    portrait = null,
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
}