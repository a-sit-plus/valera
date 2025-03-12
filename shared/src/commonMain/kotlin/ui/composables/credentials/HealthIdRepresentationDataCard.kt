package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.healthid.HealthIdScheme
import data.PersonalDataCategory
import data.credentials.HealthIdCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun HealthIdRepresentationDataCard(
    credentialAdapter: HealthIdCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = HealthIdScheme,
        personalDataCategory = PersonalDataCategory.Metadata,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        HealthIdRepresentationDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun HealthIdRepresentationDataCardContent(
    credentialAdapter: HealthIdCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        credentialAdapter.oneTimeToken?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.affiliationCountry?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.expiryDate?.let{
            AttributeRepresentation(it)
        }
    }
}