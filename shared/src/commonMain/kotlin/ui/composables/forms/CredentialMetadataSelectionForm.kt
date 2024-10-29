package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import compose_wallet_app.shared.generated.resources.issuing_label_host
import compose_wallet_app.shared.generated.resources.issuing_label_representation
import compose_wallet_app.shared.generated.resources.issuing_label_scheme
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
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_host),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(": ")
            Text(
                text = host,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        StatefulCredentialIdentifierInputField(
            value = credentialIdentifierInfo,
            onValueChange = onChangeCredentialIdentifierInfo,
            modifier = listSpacingModifier.fillMaxWidth(),
            availableIdentifiers = availableIdentifiers
        )
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_scheme),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(": ")
            Text(
                text = credentialIdentifierInfo.scheme.toScheme().uiLabel(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_representation),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(": ")
            Text(
                text = credentialIdentifierInfo.representation.uiLabel(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}