package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.PersonalDataCategory
import data.credentials.MobileDrivingLicenceCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun MobileDrivingLicenceCredentialResidenceDataCard(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = MobileDrivingLicenceScheme,
        personalDataCategory = PersonalDataCategory.ResidenceData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        MobileDrivingLicenceCredentialResidenceDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun MobileDrivingLicenceCredentialResidenceDataCardContent(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        credentialAdapter.residentAddress?.let {
            Text(it)
        }

        listOfNotNull(
            credentialAdapter.residentPostalCode,
            credentialAdapter.residentCity,
        ).any { it.isNotBlank() }.let {
            if (it) {
                AttributeRepresentation(
                    value = listOfNotNull(
                        credentialAdapter.residentPostalCode,
                        credentialAdapter.residentCity,
                    ).filter { it.isNotBlank() }.joinToString(" "),
                )
            }
        }

        listOfNotNull(
            credentialAdapter.residentCountry,
            credentialAdapter.residentState,
        ).any { it.isNotBlank() }.let {
            if (it) {
                AttributeRepresentation(
                    value = listOfNotNull(
                        credentialAdapter.residentCountry,
                        credentialAdapter.residentState,
                    ).filter { it.isNotBlank() }.joinToString(" "),
                )
            }
        }
    }
}