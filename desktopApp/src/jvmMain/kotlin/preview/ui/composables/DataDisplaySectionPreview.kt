package preview.ui.composables

import ui.composables.DataDisplaySection
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        DataDisplaySection(
            title = "Besitzer",
            data = mapOf(
                "C1.1 | Name oder Firmenname" to "Mustermann",
                "C1.2 | Vorname" to "Max",
                "C1.3 | Anschrift" to "Teststraße 12/6, 8020 Graz",
                "A.3 | Geburtsdatum/Firmenbuchnummer" to "01.02.1990",
            ).toList()
        )
    }
}
