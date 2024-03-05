package preview.ui.views

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.views.AuthenticationConsentView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AuthenticationConsentView(
            navigateUp = {},
            cancelAuthentication = {},
            consentToDataTransmission = {},
            loadMissingData = {},
            spName = "Post-Schalter#3",
            spLocation = "St. Peter Hauptstraße\n8010, Graz",
            spImage = null,
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
                PersonalDataCategory.ResidenceData to listOf(
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
            ).toList(),
            onBiometrySuccess = {},
            onBiometryDismissed = {},
            showBiometry = false,
        )
    }
}