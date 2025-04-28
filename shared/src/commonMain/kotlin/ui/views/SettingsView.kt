package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_clear_log
import at.asitplus.valera.resources.button_label_confirm
import at.asitplus.valera.resources.button_label_data_protection_policy
import at.asitplus.valera.resources.button_label_dismiss
import at.asitplus.valera.resources.button_label_faq
import at.asitplus.valera.resources.button_label_licenses
import at.asitplus.valera.resources.button_label_reset_app
import at.asitplus.valera.resources.button_label_share_log_file
import at.asitplus.valera.resources.button_label_sign
import at.asitplus.valera.resources.heading_label_settings_screen
import at.asitplus.valera.resources.reset_app_alert_text
import at.asitplus.valera.resources.section_heading_actions
import at.asitplus.valera.resources.section_heading_transfer_options
import at.asitplus.valera.resources.section_heading_information
import at.asitplus.valera.resources.switch_label_blel2cap_enabled
import at.asitplus.valera.resources.switch_label_use_ble_central_client_mode
import at.asitplus.valera.resources.switch_label_use_ble_peripheral_server_mode
import at.asitplus.valera.resources.switch_label_use_negotiated_handover
import at.asitplus.valera.resources.switch_label_use_nfc_data_transfer
import at.asitplus.valera.resources.text_label_build
import at.asitplus.valera.resources.warning
import at.asitplus.wallet.app.common.presentation.TransferSettings.Companion.transferSettings
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    vm: SettingsViewModel,
    bottomBar: @Composable () -> Unit
) {
    val showAlert = remember { mutableStateOf(false) }
    if (showAlert.value) {
        ResetAlert(
            onConfirm = {
                vm.onClickResetApp()
                showAlert.value = false
            },
            onDismiss = { showAlert.value = false },
            onDismissRequest = { showAlert.value = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_settings_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Logo(onClick = vm.onClickLogo)
                        Spacer(Modifier.width(8.dp))
                    }
                },
            )
        },
        bottomBar = { bottomBar() },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column {
                Column(
                    modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val layoutSpacingModifier = Modifier.padding(top = 24.dp)

                    Column(
                        modifier = layoutSpacingModifier
                    ) {
                        val listSpacingModifier = Modifier.padding(top = 8.dp)
                        Text(
                            text = stringResource(Res.string.section_heading_information),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_faq),
                            onClick = vm.onClickFAQs,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_data_protection_policy),
                            onClick = vm.onClickDataProtectionPolicy,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_licenses),
                            onClick = vm.onClickLicenses,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                    }

                    Column(
                        modifier = layoutSpacingModifier
                    ) {
                        val listSpacingModifier = Modifier.padding(top = 8.dp)
                        Text(
                            text = stringResource(Res.string.section_heading_actions),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_share_log_file),
                            onClick = vm.onClickShareLogFile,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_clear_log),
                            onClick = vm.onClickClearLogFile,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.SettingsBackupRestore,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_reset_app),
                            onClick = {
                                showAlert.value = true
                            },
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        TextIconButtonListItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                )
                            },
                            label = stringResource(Res.string.button_label_sign),
                            onClick = {
                                vm.onClickSigning()
                            },
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                    }

                    Column(
                        modifier = layoutSpacingModifier
                    ) {
                        val listSpacingModifier = Modifier
                        Text(
                            text = stringResource(Res.string.section_heading_transfer_options),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_negotiated_handover),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = transferSettings.presentmentUseNegotiatedHandover.collectAsState().value,
                            onCheckedChange = { checked ->
                                println("checked = ${checked}")
                                transferSettings.presentmentUseNegotiatedHandover.value = checked
                            }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_ble_central_client_mode),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = transferSettings.presentmentBleCentralClientModeEnabled.collectAsState().value,
                            onCheckedChange = { checked ->
                                transferSettings.presentmentBleCentralClientModeEnabled.value = checked
                            }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_ble_peripheral_server_mode),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = transferSettings.presentmentBlePeripheralServerModeEnabled.collectAsState().value,
                            onCheckedChange = { checked ->
                                transferSettings.presentmentBlePeripheralServerModeEnabled.value = checked
                            }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_nfc_data_transfer),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = transferSettings.presentmentNfcDataTransferEnabled.collectAsState().value,
                            onCheckedChange = { checked ->
                                transferSettings.presentmentNfcDataTransferEnabled.value = checked
                            }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_blel2cap_enabled),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = transferSettings.readerBleL2CapEnabled.collectAsState().value,
                            onCheckedChange = { checked ->
                                transferSettings.readerBleL2CapEnabled.value = checked
                            }
                        )
                    }
                }
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("${stringResource(Res.string.text_label_build)}: ${vm.version}-${vm.buildType}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TextIconButtonListItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = 16.dp
    Row(
        modifier = modifier.clickable(
            onClick = onClick,
        ).padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp),
    ) {
        icon()
        Spacer(modifier = Modifier.width(gap))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}


@Composable
private fun ResetAlert(
    onDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(stringResource(Res.string.warning))
        },
        text = {
            Text(stringResource(Res.string.reset_app_alert_text))
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(stringResource(Res.string.button_label_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.button_label_dismiss))
            }
        }
    )
}