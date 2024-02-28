package preview.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.datetime.LocalDate
import ui.composables.IdentityData
import ui.composables.PersonIdentityDataDetailCard

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonIdentityDataDetailCard(
            identityData = IdentityData(
                firstname = "Max",
                lastname = "Mustermann",
                dateOfBirth = LocalDate(year = 1990, monthNumber = 1, dayOfMonth = 1),
                portrait = null
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
