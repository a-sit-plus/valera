package previews

import ui.composables.IdentityData
import ui.composables.PersonIdentityDataDetailCard
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.datetime.LocalDate

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonIdentityDataDetailCard(
            identityData = IdentityData(
                name = "Max Mustermann",
                birthdate = LocalDate(year = 1990, monthNumber = 1, dayOfMonth = 1),
                picture = null
            ),
            onDetailClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
