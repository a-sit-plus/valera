package ui.views

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_attestation_apply
import at.asitplus.valera.resources.heading_label_attestation
import at.asitplus.valera.resources.section_heading_general
import at.asitplus.valera.resources.text_label_wallet_provider
import at.asitplus.valera.resources.text_label_wallet_provider_attestation
import at.asitplus.wallet.app.common.LoadingMessageKey
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.AttestationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttestationSettingsView(
    onClickLogo: () -> Unit,
    onClickBack: () -> Unit,
    onError: (Throwable) -> Unit,
    vm: AttestationSettingsViewModel
) {
    LaunchedEffect(null) { vm.onError.collect { onError(it) } }

    val walletProviderAttestationEnabled by vm.walletProviderAttestationEnabled.collectAsState()
    vm.getWalletProviderHost().collectAsState(null).value?.let { host ->
        var hostInput by remember { mutableStateOf(host) }
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
                        Spacer(Modifier.width(15.dp))
                    },
                    navigationIcon = {
                        NavigateUpButton(onClick = {
                            onClickBack()
                        })
                    },
                )
            }
        ) { scaffoldPadding ->
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    ) {
                        val layoutSpacingModifier = Modifier.padding(top = 24.dp)

                        Column(
                            modifier = layoutSpacingModifier
                        ) {
                            val listSpacingModifier = Modifier.padding(top = 8.dp)
                            Text(
                                text = stringResource(Res.string.section_heading_general),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                modifier = listSpacingModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    label = {
                                        Text(stringResource(Res.string.text_label_wallet_provider))
                                    },
                                    singleLine = true,
                                    readOnly = false,
                                    value = hostInput,
                                    onValueChange = { hostInput = it },
                                    enabled = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        vm.setWalletProviderHost(hostInput)
                                    }, enabled = hostInput != host
                                ) {
                                    Text(stringResource(Res.string.button_label_attestation_apply))
                                }
                            }
                            SwitchListItem(
                                label = stringResource(Res.string.text_label_wallet_provider_attestation),
                                checked = walletProviderAttestationEnabled,
                                onCheckedChange = vm::setWalletProviderAttestationEnabled,
                                modifier = listSpacingModifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    } ?: LoadingView(loadingMessageString(LoadingMessageKey.AttestationSettings))
}

@Composable
private fun SwitchListItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
