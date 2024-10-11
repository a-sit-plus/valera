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
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.ConstantIndex
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.text_label_id_format
import org.jetbrains.compose.resources.stringResource


@Composable
fun StatefulCredentialRepresentationInputField(
    value: ConstantIndex.CredentialRepresentation,
    onValueChange: ((ConstantIndex.CredentialRepresentation) -> Unit)?,
    options: List<ConstantIndex. CredentialRepresentation>,
    modifier: Modifier = Modifier,
) {
    StatefulCredentialRepresentationInputField(
        value = value,
        onValueChange = onValueChange ?: {},
        options = options,
        enabled = onValueChange != null,
        modifier = modifier,
    )
}


@Composable
fun StatefulCredentialRepresentationInputField(
    value: ConstantIndex.CredentialRepresentation,
    onValueChange: (ConstantIndex.CredentialRepresentation) -> Unit,
    options: List<ConstantIndex. CredentialRepresentation>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    CredentialRepresentationInputField(
        value = value,
        onValueChange = {
            onValueChange(it)
            showMenu = false
        },
        options = options,
        expanded = showMenu,
        onExpandedChange = {
            if (enabled) {
                showMenu = it
            }
        },
        enabled = enabled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialRepresentationInputField(
    value: ConstantIndex.CredentialRepresentation,
    onValueChange: (ConstantIndex.CredentialRepresentation) -> Unit,
    options: List<ConstantIndex. CredentialRepresentation>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange ?: {},
        modifier = modifier,
    ) {
        OutlinedTextField(
            enabled = enabled,
            readOnly = true,
            value = value.name,
            onValueChange = {},
            label = {
                Text(stringResource(Res.string.text_label_id_format))
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (credentialRepresentation in options) {
                DropdownMenuItem(
                    text = {
                        Text(credentialRepresentation.uiLabel())
                    },
                    onClick = {
                        onValueChange(credentialRepresentation)
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}