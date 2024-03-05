package preview.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.AgeData
import ui.composables.PersonAgeDataDetailCard

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonAgeDataDetailCard(
            ageData = AgeData(
                ageLowerBounds = listOf(14, 18, 16),
                ageUpperBounds = listOf(21),
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
