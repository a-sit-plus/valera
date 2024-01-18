package previews

import ui.views.LoadDataView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        LoadDataView(
            navigateUp = {},
            newDataItems = mapOf(
                "Identitätsdaten" to listOf("Vorname", "Nachname", "Geburtsdatum", "Bereichsspezifisches Personenkennzeichen"),
                "Zulassungsdaten" to listOf("Klasse A", "Klasse B"),
            ).toList(),
            renewDataItems = mapOf(
                "Identitätsdaten" to listOf("Vorname", "Nachname", "Geburtsdatum", "Bereichsspezifisches Personenkennzeichen"),
                "Zulassungsdaten" to listOf("Klasse A", "Klasse B"),
            ).toList(),
            loadData = { newData, renewData -> }
        )
    }
}
