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
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.text_label_id_scheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulCredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: ((ConstantIndex.CredentialScheme) -> Unit)?,
    modifier: Modifier = Modifier,
    availableSchemes: List<ConstantIndex.CredentialScheme>,
) {
    StatefulCredentialSchemeInputField(
        value = value,
        onValueChange = onValueChange ?: {},
        enabled = onValueChange != null,
        modifier = modifier,
        availableSchemes = availableSchemes,
    )
}

@Composable
fun StatefulCredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: (ConstantIndex.CredentialScheme) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    availableSchemes: List<ConstantIndex.CredentialScheme>,
) {
    var showMenu by remember { mutableStateOf(false) }

    CredentialSchemeInputField(
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
        availableSchemes = availableSchemes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: (ConstantIndex.CredentialScheme) -> Unit,
    expanded: Boolean,
    enabled: Boolean = true,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    availableSchemes: List<ConstantIndex.CredentialScheme>,
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
            label = {
                Text(stringResource(Res.string.text_label_id_scheme))
            },
            enabled = enabled,
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
            for (scheme in availableSchemes) {
                DropdownMenuItem(
                    text = {
                        Text(scheme.uiLabel())
                    },
                    onClick = {
                        onValueChange(scheme)
                    },
                    enabled = enabled,
                )
            }
        }
    }
}