package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import data.PersonalDataCategory
import data.credentials.EPrescriptionCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun EPrescriptionRepresentationDataCard(
    credentialAdapter: EPrescriptionCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = EPrescriptionScheme,
        personalDataCategory = PersonalDataCategory.Metadata,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        EPrescriptionRepresentationDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun EPrescriptionRepresentationDataCardContent(
    credentialAdapter: EPrescriptionCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        credentialAdapter.ott?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.countryCode?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.validUntil?.let{
            AttributeRepresentation(it)
        }
    }
}