package ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
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
import at.asitplus.valera.resources.error_clearing_log_failed
import at.asitplus.valera.resources.error_feature_not_yet_available
import at.asitplus.valera.resources.error_resetting_app_failed
import at.asitplus.valera.resources.eu_normal_reproduction_low_resolution
import at.asitplus.valera.resources.heading_label_settings_screen
import at.asitplus.valera.resources.info_text_co_founded_by_eu
import at.asitplus.valera.resources.info_text_received_funding_from_eu
import at.asitplus.valera.resources.reset_app_alert_text
import at.asitplus.valera.resources.section_heading_actions
import at.asitplus.valera.resources.section_heading_information
import at.asitplus.valera.resources.section_heading_transfer_options
import at.asitplus.valera.resources.switch_label_blel2cap_enabled
import at.asitplus.valera.resources.switch_label_use_ble_central_client_mode
import at.asitplus.valera.resources.switch_label_use_ble_peripheral_server_mode
import at.asitplus.valera.resources.switch_label_use_negotiated_handover
import at.asitplus.valera.resources.switch_label_use_nfc_data_transfer
import at.asitplus.valera.resources.text_label_build
import at.asitplus.valera.resources.warning
import at.asitplus.wallet.app.common.BuildType
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.composables.CircularProgressIndicatorOverlay
import ui.composables.DelayedComposable
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.SettingsViewModel
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    buildType: BuildType,
    version: String,
    onClickShareLogFile: () -> Unit,
    onClickLogo: () -> Unit,
    onClickBack: () -> Unit,
    onClickSettings: () -> Unit,
    onClickFAQs: (() -> Unit)?,
    onClickDataProtectionPolicy: (() -> Unit)?,
    onClickLicenses: (() -> Unit)?,
    koinScope: Scope,
    settingsViewModel: SettingsViewModel = koinViewModel(scope = koinScope),
) {
    var isLoading by rememberSaveable {
        mutableStateOf(false)
    }

    var showResetAlert by remember { mutableStateOf(false) }
    if (showResetAlert) {
        ResetAlert(
            onConfirm = {
                showResetAlert = false
                isLoading = true
                settingsViewModel.onClickResetApp {
                    isLoading = false
                    if (it != null) {
                        settingsViewModel.showGlobalSnackbar {
                            getString(Res.string.error_resetting_app_failed)
                        }
                    }
                }
            },
            onDismiss = { showResetAlert = false },
            onDismissRequest = { showResetAlert = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_settings_screen),
                            Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = {
                    NavigateUpButton(onClickBack)
                },
            )
        }
    ) { scaffoldPadding ->
        if(isLoading) {
            DelayedComposable(1.seconds) {
                CircularProgressIndicatorOverlay()
            }
        }
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
                            onClick = {
                                onClickFAQs?.invoke() ?: settingsViewModel.showGlobalSnackbar {
                                    getString(Res.string.error_feature_not_yet_available)
                                }
                            },
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
                            onClick = {
                                onClickDataProtectionPolicy?.invoke() ?: settingsViewModel.showGlobalSnackbar {
                                    getString(Res.string.error_feature_not_yet_available)
                                }
                            },
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
                            onClick = {
                                onClickLicenses?.invoke() ?: settingsViewModel.showGlobalSnackbar {
                                    getString(Res.string.error_feature_not_yet_available)
                                }
                            },
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
                            onClick = onClickShareLogFile,
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
                            onClick = {
                                settingsViewModel.onClickClearLogFile {
                                    if (it != null) {
                                        settingsViewModel.showGlobalSnackbar {
                                            getString(Res.string.error_clearing_log_failed)
                                        }
                                    }
                                }
                            },
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
                                showResetAlert = true
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
                            isChecked = settingsViewModel.presentmentUseNegotiatedHandover.collectAsState().value,
                            onCheckedChange = { settingsViewModel.setPresentmentUseNegotiatedHandover(it) }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_ble_central_client_mode),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = settingsViewModel.presentmentBleCentralClientModeEnabled.collectAsState().value,
                            onCheckedChange = { settingsViewModel.setPresentmentBleCentralClientModeEnabled(it) }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_ble_peripheral_server_mode),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = settingsViewModel.presentmentBlePeripheralServerModeEnabled.collectAsState().value,
                            onCheckedChange = { settingsViewModel.setPresentmentBlePeripheralServerModeEnabled(it) }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_use_nfc_data_transfer),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = settingsViewModel.presentmentNfcDataTransferEnabled.collectAsState().value,
                            onCheckedChange = { settingsViewModel.setPresentmentNfcDataTransferEnabled(it) }
                        )
                        SettingSwitch(
                            label = stringResource(Res.string.switch_label_blel2cap_enabled),
                            modifier = listSpacingModifier.fillMaxWidth(),
                            isChecked = settingsViewModel.readerBleL2CapEnabled.collectAsState().value,
                            onCheckedChange = { settingsViewModel.setReaderBleL2CapEnabled(it) }
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp)
                        .padding(top = 40.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.eu_normal_reproduction_low_resolution),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.width(125.dp)
                            )
                            Text(stringResource(Res.string.info_text_co_founded_by_eu), textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(Res.string.info_text_received_funding_from_eu),
                            textAlign = TextAlign.Justify
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            .fillMaxSize()
                    ) {
                        Text("${stringResource(Res.string.text_label_build)}: ${version}-${buildType}")
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