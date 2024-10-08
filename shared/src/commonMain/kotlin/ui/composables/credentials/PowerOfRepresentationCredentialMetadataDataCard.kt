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
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.PersonalDataCategory
import data.credentials.PowerOfRepresentationCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun PowerOfRepresentationCredentialMetaDataCard(
    credentialAdapter: PowerOfRepresentationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = PowerOfRepresentationScheme,
        personalDataCategory = PersonalDataCategory.Metadata,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        PowerOfRepresentationCredentialMetaDataDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun PowerOfRepresentationCredentialMetaDataDataCardContent(
    credentialAdapter: PowerOfRepresentationCredentialAdapter,
    modifier: Modifier = Modifier
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
            credentialAdapter.issuanceDate?.let { AttributeRepresentation(it) }
            credentialAdapter.expiryDate?.let { AttributeRepresentation(it) }
            credentialAdapter.issuingCountry?.let { AttributeRepresentation(it) }
            credentialAdapter.issuingAuthority?.let { AttributeRepresentation(it) }
            credentialAdapter.issuingJurisdiction?.let { AttributeRepresentation(it) }
            credentialAdapter.documentNumber?.let { AttributeRepresentation(it) }
            credentialAdapter.administrativeNumber?.let { AttributeRepresentation(it) }
        }
    }
}