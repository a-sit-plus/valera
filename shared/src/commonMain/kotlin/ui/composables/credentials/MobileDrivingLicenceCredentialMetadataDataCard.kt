package ui.composables.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_portrait
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeRepresentation

@Composable
fun MobileDrivingLicenceCredentialMetadataCard(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = MobileDrivingLicenceScheme,
        personalDataCategory = PersonalDataCategory.Metadata,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        MobileDrivingLicenceCredentialMetadataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun MobileDrivingLicenceCredentialMetadataCardContent(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
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
            credentialAdapter.documentNumber?.let { AttributeRepresentation(it) }
            credentialAdapter.issueDate?.let { AttributeRepresentation(it) }
            credentialAdapter.expiryDate?.let { AttributeRepresentation(it) }
        }
    }
}