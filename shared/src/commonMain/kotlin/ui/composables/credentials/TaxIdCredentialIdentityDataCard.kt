package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.taxid.TaxIdScheme
import data.PersonalDataCategory
import data.credentials.TaxIdCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun TaxIdCredentialIdentityDataCard(
    credentialAdapter: TaxIdCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = TaxIdScheme,
        personalDataCategory = PersonalDataCategory.IdentityData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        TaxIdCredentialIdentityDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun TaxIdCredentialIdentityDataCardContent(
    credentialAdapter: TaxIdCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        credentialAdapter.registeredFamilyName?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
        credentialAdapter.registeredGivenName?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
    }
}