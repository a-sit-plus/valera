package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_load_attestation
import at.asitplus.valera.resources.heading_label_attestation
import at.asitplus.valera.resources.text_label_attestation_expiration
import at.asitplus.valera.resources.text_label_attestation_issued
import at.asitplus.valera.resources.text_label_attestation_storage_type
import at.asitplus.valera.resources.text_label_instance_attestation
import at.asitplus.valera.resources.text_label_unit_attestation
import at.asitplus.valera.resources.text_label_wallet_provider
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledText
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.AttestationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttestationSettingsView(
    onClickLogo: () -> Unit,
    onClickBack: () -> Unit,
    onClickSettings: () -> Unit,
    onError: (Throwable) -> Unit,
    vm: AttestationSettingsViewModel
) {
    LaunchedEffect(null) { vm.onError.collect { onError(it) } }

    val bufferedInstanceAttestation = vm.attestationService.bufferedInstanceAttestation.collectAsState(null)
    val bufferedUnitAttestation = vm.attestationService.bufferedUnitAttestation.collectAsState(null)

    vm.attestationService.getWalletProviderHost().collectAsState(null).value?.let { it ->
        var host by remember { mutableStateOf(it) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ScreenHeading(
                                stringResource(Res.string.heading_label_attestation),
                                Modifier.weight(1f)
                            )
                        }
                    },
                    actions = {
                        Logo(onClick = onClickLogo)
                        Column(modifier = Modifier.clickable(onClick = {
                            vm.attestationService.setWalletProviderHost(host)
                            onClickSettings()
                        })) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                            )
                        }
                        Spacer(Modifier.width(15.dp))
                    },
                    navigationIcon = {
                        NavigateUpButton(onClick = {
                            vm.attestationService.setWalletProviderHost(host)
                            onClickBack()
                        })
                    },
                )
            }
        ) { scaffoldPadding ->
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        label = {
                            Text(stringResource(Res.string.text_label_wallet_provider))
                        },
                        singleLine = true,
                        readOnly = false,
                        value = host,
                        onValueChange = { host = it },
                        enabled = true,
                        modifier = Modifier,
                    )
                    Spacer(Modifier.height(10.dp))
                    bufferedInstanceAttestation.value?.let {
                        Text(stringResource(Res.string.text_label_instance_attestation), fontWeight = FontWeight.Bold)
                        LabeledText(
                            text = "${it.payload.issuedAt}",
                            label = stringResource(Res.string.text_label_attestation_issued)
                        )
                        LabeledText(
                            text = "${it.payload.expiration}",
                            label = stringResource(Res.string.text_label_attestation_expiration)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    bufferedUnitAttestation.value?.let {
                        Text(stringResource(Res.string.text_label_unit_attestation), fontWeight = FontWeight.Bold)
                        LabeledText(
                            text = "${it.payload.issuedAt}",
                            label = stringResource(Res.string.text_label_attestation_issued)
                        )
                        LabeledText(
                            text = "${it.payload.expiration}",
                            label = stringResource(Res.string.text_label_attestation_expiration)
                        )
                        LabeledText(
                            text = it.payload.keyStorage?.joinToString().orEmpty(),
                            label = stringResource(Res.string.text_label_attestation_storage_type)
                        )
                    }
                    if (bufferedUnitAttestation.value == null || bufferedInstanceAttestation.value == null) {
                        Button(onClick = {
                            vm.attestationService.setWalletProviderHost(host)
                            vm.preload()
                        }) {
                            Text(stringResource(Res.string.button_label_load_attestation))
                        }
                    }
                }
            }
        }
    } ?: LoadingView()
}
