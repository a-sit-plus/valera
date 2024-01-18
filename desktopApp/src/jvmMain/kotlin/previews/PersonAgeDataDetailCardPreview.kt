package previews

import ui.composables.AgeData
import ui.composables.PersonAgeDataDetailCard
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonAgeDataDetailCard(
            ageData = AgeData(
                ageLowerBounds = listOf(14, 18, 16),
                ageUpperBounds = listOf(20),
            ),
            onDetailClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
