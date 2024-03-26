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
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.text_label_id_format
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulCredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: (ConstantIndex.CredentialScheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    CredentialSchemeInputField(
        value = value,
        onValueChange = {
            onValueChange(it)
            showMenu = false
        },
        expanded = showMenu,
        onExpandedChange = {
            showMenu = it
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun CredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: (ConstantIndex.CredentialScheme) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = value.vcType,
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
                    Text(IdAustriaScheme.vcType)
                },
                onClick = {
                    onValueChange(IdAustriaScheme)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(EuPidScheme.vcType)
                },
                onClick = {
                    onValueChange(EuPidScheme)
                },
            )
        }
    }
}