package ui.composables.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.asitplus.wallet.eupid.EuPidScheme
import data.PersonalDataCategory
import data.credentials.EuPidCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun EuPidCredentialIdentityDataCard(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = EuPidScheme,
        personalDataCategory = PersonalDataCategory.IdentityData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        EuPidCredentialIdentityDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun EuPidCredentialIdentityDataCardContent(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    var columnSize by remember { mutableStateOf(Size.Zero) }
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier.fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
            columnSize = layoutCoordinates.size.toSize()
        },
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            val spacingModifier = Modifier.padding(bottom = 4.dp)
            AttributeRepresentation(
                value = listOfNotNull(
                    credentialAdapter.givenName,
                    credentialAdapter.familyName,
                ).joinToString(" "),
                modifier = spacingModifier,
            )
            credentialAdapter.birthDate?.let {
                AttributeRepresentation(
                    value = it,
                    modifier = spacingModifier,
                )
            }
            credentialAdapter.nationality?.let {
                AttributeRepresentation(
                    value = it,
                    modifier = spacingModifier,
                )
            } ?: credentialAdapter.nationalities?.let {
                AttributeRepresentation(
                    value = it,
                    modifier = spacingModifier,
                )
            }
        }
    }
}
