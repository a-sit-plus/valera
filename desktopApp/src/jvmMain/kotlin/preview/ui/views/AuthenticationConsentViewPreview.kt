package preview.ui.views

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.views.AuthenticationConsentView

@OptIn(ExperimentalMaterial3Api::class)
private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var showBottomSheet by remember { mutableStateOf(false) }
        val bottomSheetState = rememberModalBottomSheetState()

        AuthenticationConsentView(
            navigateUp = {},
            cancelAuthentication = {},
            consentToDataTransmission = {
                showBottomSheet = true
            },
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
        )
    }
}