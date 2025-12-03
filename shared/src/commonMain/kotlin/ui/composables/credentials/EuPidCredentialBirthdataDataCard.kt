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
import data.PersonalDataCategory
import data.credentials.EuPidCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun EuPidCredentialBirthdataDataCard(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = credentialAdapter.scheme,
        personalDataCategory = PersonalDataCategory.BirthData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        EuPidCredentialBirthdataDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun EuPidCredentialBirthdataDataCardContent(
    credentialAdapter: EuPidCredentialAdapter,
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
            credentialAdapter.givenNameBirth?.let { AttributeRepresentation(it) }
            credentialAdapter.familyNameBirth?.let { AttributeRepresentation(it) }
            credentialAdapter.placeOfBirth?.let { AttributeRepresentation(it) }
            credentialAdapter.birthPlace?.let { AttributeRepresentation(it) }
            credentialAdapter.birthCity?.let { AttributeRepresentation(it) }
            credentialAdapter.birthCountry?.let { AttributeRepresentation(it) }
            credentialAdapter.birthState?.let { AttributeRepresentation(it) }
        }
    }
}