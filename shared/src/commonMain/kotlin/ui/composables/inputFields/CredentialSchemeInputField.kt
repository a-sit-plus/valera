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
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.credential_scheme_label_certificate_of_residence
import compose_wallet_app.shared.generated.resources.credential_scheme_label_eu_pid
import compose_wallet_app.shared.generated.resources.credential_scheme_label_id_austria
import compose_wallet_app.shared.generated.resources.credential_scheme_label_mdl
import compose_wallet_app.shared.generated.resources.credential_scheme_label_power_of_representation
import compose_wallet_app.shared.generated.resources.text_label_id_scheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulCredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: ((ConstantIndex.CredentialScheme) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    StatefulCredentialSchemeInputField(
        value = value,
        onValueChange = onValueChange ?: {},
        enabled = onValueChange != null,
        modifier = modifier,
    )
}

@Composable
fun StatefulCredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: (ConstantIndex.CredentialScheme) -> Unit,
    enabled: Boolean = true,
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
            if (enabled) {
                showMenu = it
            }
        },
        enabled = enabled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun CredentialSchemeInputField(
    value: ConstantIndex.CredentialScheme,
    onValueChange: (ConstantIndex.CredentialScheme) -> Unit,
    expanded: Boolean,
    enabled: Boolean = true,
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
            for (scheme in listOf(
                MobileDrivingLicenceScheme,
                IdAustriaScheme,
                EuPidScheme,
                CertificateOfResidenceScheme,
                PowerOfRepresentationScheme,
                EPrescriptionScheme
            )) {
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