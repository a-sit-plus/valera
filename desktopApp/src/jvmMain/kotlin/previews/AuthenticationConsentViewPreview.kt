package previews

import ui.views.AuthenticationConsentView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationConsentView(
            navigateUp = {},
            cancelAuthentication = {},
            consentToDataTransmission = {},
            loadMissingData = {},
            spName = "Post-Schalter#3",
            spLocation = "St. Peter Hauptstraße\n8010, Graz",
            requestedAttributes = mapOf(
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