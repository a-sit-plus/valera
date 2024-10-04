package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.PersonalDataCategory
import data.credentials.MobileDrivingLicenceCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun MobileDrivingLicenceCredentialDrivingPermissionsDataCard(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = MobileDrivingLicenceScheme,
        personalDataCategory = PersonalDataCategory.DrivingPermissions,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        MobileDrivingLicenceCredentialDrivingPermissionsDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun MobileDrivingLicenceCredentialDrivingPermissionsDataCardContent(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        credentialAdapter.drivingPrivileges?.let { privileges ->
            AttributeRepresentation(value = privileges.joinToString(separator = " - ") { it.vehicleCategoryCode })
        }
    }
}