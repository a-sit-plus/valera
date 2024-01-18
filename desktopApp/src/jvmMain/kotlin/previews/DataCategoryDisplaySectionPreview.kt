package previews

import ui.composables.AttributeAvailability
import ui.composables.DataCategoryDisplaySection
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.PersonalDataCategory

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        DataCategoryDisplaySection(
            title = "Angefragte Daten",
            attributes = mapOf(
                PersonalDataCategory.IdentityData to listOf(
                    AttributeAvailability(
                        attributeName = "Vorname",
                        isAvailable = false,
                    ),
                    AttributeAvailability(
                        attributeName = "Nachname",
                        isAvailable = false,
                    ),
                    AttributeAvailability(
                        attributeName = "Aktuelles Foto aus zentralem Identitätsdokumentenregister",
                        isAvailable = false,
                    ),
                ),
                PersonalDataCategory.ResidencyData to listOf(
                    AttributeAvailability(
                        attributeName = "Straße",
                        isAvailable = false,
                    ),
                    AttributeAvailability(
                        attributeName = "Hausnummer",
                        isAvailable = false,
                    ),
                    AttributeAvailability(
                        attributeName = "Postleitzahl",
                        isAvailable = true,
                    ),
                    AttributeAvailability(
                        attributeName = "Ort",
                        isAvailable = true,
                    ),
                ),
            ).toList()
        )
    }
}
