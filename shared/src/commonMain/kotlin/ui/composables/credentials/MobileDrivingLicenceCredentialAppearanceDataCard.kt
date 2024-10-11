package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.PersonalDataCategory
import data.credentials.MobileDrivingLicenceCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun MobileDrivingLicenceCredentialAppearanceDataCard(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = MobileDrivingLicenceScheme,
        personalDataCategory = PersonalDataCategory.AppearanceData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        MobileDrivingLicenceCredentialAppearanceDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun MobileDrivingLicenceCredentialAppearanceDataCardContent(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        listOfNotNull(
            credentialAdapter.sex,
            credentialAdapter.eyeColour,
            credentialAdapter.hairColour,
        ).let {
            AttributeRepresentation(
                value = listOfNotNull(
                    credentialAdapter.sex,
                    credentialAdapter.eyeColour,
                    credentialAdapter.hairColour,
                ).joinToString(" | "),
            )
        }
    }
}