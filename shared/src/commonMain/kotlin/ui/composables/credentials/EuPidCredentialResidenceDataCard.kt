package ui.composables.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_portrait
import data.PersonalDataCategory
import data.credentials.EuPidCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource
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