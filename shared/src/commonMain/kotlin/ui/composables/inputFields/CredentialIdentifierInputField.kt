package ui.composables.inputFields

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.CredentialIdentifierInfo
import at.asitplus.wallet.app.common.credentialScheme
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.oidvci.toRepresentation
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.text_label_id_identifier
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulCredentialIdentifierInputField(
    value: CredentialIdentifierInfo,
    onValueChange: ((CredentialIdentifierInfo) -> Unit)?,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
) {
    StatefulCredentialIdentifierInputField(
        value = value,
        onValueChange = onValueChange ?: {},
        enabled = onValueChange != null,
        modifier = modifier,
        availableIdentifiers = availableIdentifiers,
    )
}

@Composable
fun StatefulCredentialIdentifierInputField(
    value: CredentialIdentifierInfo,
    onValueChange: (CredentialIdentifierInfo) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
) {
    var showMenu by remember { mutableStateOf(false) }

    CredentialIdentifierInputField(
        value = value,
        onValueChange = {
            onValueChange(it)
            showMenu = false
        },
        expanded = showMenu,
        enabled = enabled,
        onExpandedChange = { if (enabled) { showMenu = it } },
        modifier = modifier,
        availableIdentifiers = availableIdentifiers,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialIdentifierInputField(
    value: CredentialIdentifierInfo,
    onValueChange: (CredentialIdentifierInfo) -> Unit,
    expanded: Boolean,
    enabled: Boolean = true,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = value.uiLabel(),
            onValueChange = {},
            label = { Text(stringResource(Res.string.text_label_id_identifier)) },
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (identifier in availableIdentifiers) {
                DropdownMenuItem(
                    text = { Text(identifier.uiLabel()) },
                    onClick = { onValueChange(identifier) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun CredentialIdentifierInfo.uiLabel() =
    "${credentialScheme?.uiLabel() ?: "Unknown"} (${supportedCredentialFormat.format.toRepresentation().uiLabel()})"