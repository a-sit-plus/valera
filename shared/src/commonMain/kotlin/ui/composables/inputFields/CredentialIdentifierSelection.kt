package ui.composables.inputFields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.warning_issuer_not_registered_for_credential
import at.asitplus.wallet.app.common.resolveCredentialScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.app.common.validation.IssuerValidationUi
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.oidvci.CredentialIdentifierInfo
import at.asitplus.wallet.lib.oidvci.toRepresentation
import org.jetbrains.compose.resources.stringResource

/**
 * Lets the user select any of [availableIdentifiers] with a checkbox each, showing its scheme and representation, and
 * whether the [issuer] is not registered for it. [onSelectedChange] receives the selection in the order of
 * [availableIdentifiers].
 */
@Composable
fun CredentialIdentifierSelection(
    selected: List<CredentialIdentifierInfo>,
    onSelectedChange: (List<CredentialIdentifierInfo>) -> Unit,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    issuer: IssuerValidationUi? = null,
) {
    // produceState bridges the suspend resolveCredentialScheme() into Compose state
    val schemes by produceState(emptyMap<CredentialIdentifierInfo, CredentialScheme?>(), availableIdentifiers) {
        value = availableIdentifiers.associateWith { it.supportedCredentialFormat.resolveCredentialScheme() }
    }
    Column(modifier = modifier) {
        availableIdentifiers.forEach { identifier ->
            val checked = identifier in selected
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { check ->
                        onSelectedChange(availableIdentifiers.filter { if (it == identifier) check else it in selected })
                    },
                ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = schemes[identifier].uiLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = identifier.supportedCredentialFormat.format.toRepresentation().uiLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (issuer?.registeredFor(identifier.supportedCredentialFormat) == false) {
                        Text(
                            text = stringResource(Res.string.warning_issuer_not_registered_for_credential),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
