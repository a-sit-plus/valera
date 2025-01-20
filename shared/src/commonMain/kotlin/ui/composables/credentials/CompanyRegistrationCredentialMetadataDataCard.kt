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
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import data.PersonalDataCategory
import data.credentials.CompanyRegistrationCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun CompanyRegistrationCredentialMetaDataCard(
    credentialAdapter: CompanyRegistrationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = CompanyRegistrationScheme,
        personalDataCategory = PersonalDataCategory.Metadata,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        CompanyRegistrationCredentialMetaDataDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun CompanyRegistrationCredentialMetaDataDataCardContent(
    credentialAdapter: CompanyRegistrationCredentialAdapter,
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
            credentialAdapter.registrationDate?.let { AttributeRepresentation(it) }
            credentialAdapter.companyEndDate?.let { AttributeRepresentation(it) }
            credentialAdapter.companyEuid?.let { AttributeRepresentation(it) }
            credentialAdapter.vatNumber?.let { AttributeRepresentation(it) }
            credentialAdapter.contactData?.let { AttributeRepresentation(it) }
            credentialAdapter.registeredAddress?.let { AttributeRepresentation(it) }
            credentialAdapter.postalAddress?.let { AttributeRepresentation(it) }
            credentialAdapter.branch?.let { AttributeRepresentation(it) }
        }
    }
}