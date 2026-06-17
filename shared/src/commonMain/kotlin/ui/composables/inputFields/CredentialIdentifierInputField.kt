package ui.composables.inputFields

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.text_label_id_identifier
import at.asitplus.wallet.app.common.resolveCredentialScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
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
        onExpandedChange = {
            if (enabled) {
                showMenu = it
            }
        },
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
    // ponytail: produceState bridges the suspend resolveCredentialScheme() into Compose state
    val schemes by produceState<Map<CredentialIdentifierInfo, CredentialScheme?>>(
        initialValue = emptyMap(),
        availableIdentifiers,
        value,
    ) {
        this.value = (availableIdentifiers + value)
            .associateWith { it.supportedCredentialFormat.resolveCredentialScheme() }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = schemes[value].uiLabel(),
            onValueChange = {},
            label = { Text(stringResource(Res.string.text_label_id_identifier)) },
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            availableIdentifiers.map { identifier ->
                DropdownMenuItem(
                    text = { Text(schemes[identifier].uiLabel()) },
                    onClick = { onValueChange(identifier) },
                    enabled = enabled,
                )
            }
        }
    }
}

