package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.eupid.EuPidScheme
import data.PersonalDataCategory
import data.credentials.EuPidCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun EuPidCredentialResidenceDataCard(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = EuPidScheme,
        personalDataCategory = PersonalDataCategory.ResidenceData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        EuPidCredentialResidenceDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun EuPidCredentialResidenceDataCardContent(
    credentialAdapter: EuPidCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        listOfNotNull(
            credentialAdapter.residentStreet,
            credentialAdapter.residentHouseNumber,
        ).any { it.isNotBlank() }.let {
            if (it) {
                val firstAddressLine = listOfNotNull(
                    credentialAdapter.residentStreet,
                    credentialAdapter.residentHouseNumber,
                ).filter {
                    it.isNotBlank()
                }.joinToString(" ")

                AttributeRepresentation(
                    value = firstAddressLine,
                )
            }
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