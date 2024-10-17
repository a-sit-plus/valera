package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.PersonalDataCategory
import data.credentials.PowerOfRepresentationCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun PowerOfRepresentationCredentialRepresentationDataCard(
    credentialAdapter: PowerOfRepresentationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = PowerOfRepresentationScheme,
        personalDataCategory = PersonalDataCategory.RepresentationData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        PowerOfRepresentationCredentialRepresentationDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun PowerOfRepresentationCredentialRepresentationDataCardContent(
    credentialAdapter: PowerOfRepresentationCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        credentialAdapter.legalPersonIdentifier?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.legalName?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        if ((credentialAdapter.effectiveFromDate ?: credentialAdapter.effectiveUntilDate) != null) {
            Row(modifier = spacingModifier,) {
                credentialAdapter.effectiveFromDate?.let {
                    AttributeRepresentation(it)
                }
                if(credentialAdapter.effectiveFromDate != null && credentialAdapter.effectiveUntilDate != null) {
                    VerticalDivider()
                    Text(" — ")
                    VerticalDivider()
                }
                credentialAdapter.effectiveUntilDate?.let {
                    AttributeRepresentation(it)
                }
            }
        }
    }
}