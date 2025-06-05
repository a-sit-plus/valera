package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.ehic.EhicScheme
import data.PersonalDataCategory
import data.credentials.EhicCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun EhicRepresentationDataCard(
    credentialAdapter: EhicCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = EhicScheme,
        personalDataCategory = PersonalDataCategory.Metadata,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        EhicRepresentationDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun EhicRepresentationDataCardContent(
    credentialAdapter: EhicCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        credentialAdapter.issuingCountry?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.issuingAuthority?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.documentNumber?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.socialSecurityNumber?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.personalAdministrativeNumber?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.issuanceDate?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.expiryDate?.let{
            AttributeRepresentation(it)
        }
    }
}