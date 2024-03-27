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
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.id_format_iso_mdoc_label
import composewalletapp.shared.generated.resources.id_format_plain_jwt_label
import composewalletapp.shared.generated.resources.id_format_sd_jwt_label
import composewalletapp.shared.generated.resources.text_label_id_format
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource


@Composable
fun StatefulCredentialRepresentationInputField(
    value: ConstantIndex.CredentialRepresentation,
    onValueChange: (ConstantIndex.CredentialRepresentation) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    CredentialRepresentationInputField(
        value = value,
        onValueChange = {
            onValueChange(it)
            showMenu = false
        },
        expanded = showMenu,
        onExpandedChange = {
            showMenu = it
        },
        enabled = enabled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun CredentialRepresentationInputField(
    value: ConstantIndex.CredentialRepresentation,
    onValueChange: (ConstantIndex.CredentialRepresentation) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
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
            DropdownMenuItem(
                text = {
                    Text(stringResource(Res.string.id_format_plain_jwt_label))
                },
                onClick = {
                    onValueChange(ConstantIndex.CredentialRepresentation.PLAIN_JWT)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(Res.string.id_format_sd_jwt_label))
                },
                onClick = {
                    onValueChange(ConstantIndex.CredentialRepresentation.SD_JWT)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(Res.string.id_format_iso_mdoc_label))
                },
                onClick = {
                    onValueChange(ConstantIndex.CredentialRepresentation.ISO_MDOC)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}