package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.section_heading_configuration
import org.jetbrains.compose.resources.stringResource
import ui.composables.inputFields.StatefulCredentialIdentifierInputField


@Composable
fun CredentialMetadataSelectionForm(
    host: String,
    credentialIdentifierInfo: ProvisioningService.CredentialIdentifierInfo,
    onChangeCredentialIdentifierInfo: (ProvisioningService.CredentialIdentifierInfo) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<ProvisioningService.CredentialIdentifierInfo>,
) {
    Column(
        modifier = modifier,
    ) {
        val listSpacingModifier = Modifier.padding(top = 8.dp)
        Text(
            text = stringResource(Res.string.section_heading_configuration),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = host,
            style = MaterialTheme.typography.bodyMedium,
        )
        StatefulCredentialIdentifierInputField(
            value = credentialIdentifierInfo,
            onValueChange = onChangeCredentialIdentifierInfo,
            modifier = listSpacingModifier.fillMaxWidth(),
            availableIdentifiers = availableIdentifiers
        )
        Text(
            text = credentialIdentifierInfo.scheme.uiLabel(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = credentialIdentifierInfo.representation.uiLabel(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}