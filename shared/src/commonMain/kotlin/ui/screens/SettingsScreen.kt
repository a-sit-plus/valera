package ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.button_label_confirm
import composewalletapp.shared.generated.resources.button_label_data_protection_policy
import composewalletapp.shared.generated.resources.button_label_dismiss
import composewalletapp.shared.generated.resources.button_label_faq
import composewalletapp.shared.generated.resources.button_label_licenses
import composewalletapp.shared.generated.resources.button_label_reset_app
import composewalletapp.shared.generated.resources.button_label_share_log_file
import composewalletapp.shared.generated.resources.error_feature_not_yet_available
import composewalletapp.shared.generated.resources.heading_label_settings_screen
import composewalletapp.shared.generated.resources.id_format_iso_mdoc_label
import composewalletapp.shared.generated.resources.id_format_plain_jwt_label
import composewalletapp.shared.generated.resources.id_format_sd_jwt_label
import composewalletapp.shared.generated.resources.reset_app_alert_text
import composewalletapp.shared.generated.resources.section_heading_actions
import composewalletapp.shared.generated.resources.section_heading_configuration
import composewalletapp.shared.generated.resources.section_heading_information
import composewalletapp.shared.generated.resources.text_label_build
import composewalletapp.shared.generated.resources.text_label_id_format
import composewalletapp.shared.generated.resources.text_label_id_scheme
import composewalletapp.shared.generated.resources.text_label_issuing_service
import composewalletapp.shared.generated.resources.text_label_stage
import composewalletapp.shared.generated.resources.warning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.SaveButton

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SettingsScreen(
    navigateToLogPage: () -> Unit,
    onClickResetApp: () -> Unit,
    walletMain: WalletMain,
) {
    val stage = Configuration.BUILD_FOR_STAGE
    val buildType = walletMain.buildContext.buildType
    val version = walletMain.buildContext.versionName

    val originalCredentialRepresentation by walletMain.walletConfig.credentialRepresentation.collectAsState(
        null
    )
    var credentialRepresentation by rememberSaveable(originalCredentialRepresentation) {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.credentialRepresentation.first())
        }
    }

    val originalHost by walletMain.walletConfig.host.collectAsState(null)
    var host by rememberSaveable {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.host.first())
        }
    }

    val originalCredentialScheme by walletMain.walletConfig.credentialScheme.map {
        it.vcType
    }.collectAsState(null)
    var credentialSchemeVcType by rememberSaveable {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.credentialScheme.first().vcType)
        }
    }

    val isSaveEnabled =
        remember(
            originalHost,
            host,
            originalCredentialRepresentation,
            credentialRepresentation,
            originalCredentialScheme,
            credentialSchemeVcType
        ) {
            host != originalHost || credentialRepresentation != originalCredentialRepresentation || credentialSchemeVcType != originalCredentialScheme
        }

    SettingsView(
        host = host,
        onChangeHost = {
            host = it
        },
        credentialRepresentation = credentialRepresentation,
        onChangeCredentialRepresentation = {
            credentialRepresentation = it
        },
        credentialSchemeVcType = credentialSchemeVcType,
        onChangeCredentialSchemeVcType = {
            credentialSchemeVcType = it
        },
        isSaveEnabled = isSaveEnabled,
        onClickSaveConfiguration = {
            walletMain.walletConfig.set(
                host = host,
                credentialRepresentation = credentialRepresentation,
                credentialSchemeVcType = credentialSchemeVcType,
            )
        },
        buildType = buildType,
        stage = stage,
        version = version,
        onClickFAQs = {
            runBlocking {
                walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
            }
        },
        onClickDataProtectionPolicy = {
            runBlocking {
                walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
            }
        },
        onClickLicenses = {
            runBlocking {
                walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
            }
        },
        onClickShareLogFile = navigateToLogPage,
        onClickResetApp = onClickResetApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun SettingsView(
    host: String,
    onChangeHost: (String) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    credentialSchemeVcType: String,
    onChangeCredentialSchemeVcType: (String) -> Unit,
    isSaveEnabled: Boolean,
    onClickSaveConfiguration: () -> Unit,
    stage: String,
    version: String,
    buildType: String,
    onClickFAQs: () -> Unit,
    onClickDataProtectionPolicy: () -> Unit,
    onClickLicenses: () -> Unit,
    onClickShareLogFile: () -> Unit,
    onClickResetApp: () -> Unit,
) {
    var showCredentialRepresentationMenu by remember { mutableStateOf(false) }
    var showCredentialSchemeMenu by remember { mutableStateOf(false) }

    val showAlert = remember { mutableStateOf(false) }
    if (showAlert.value) {
        ResetAlert(
            onConfirm = {
                onClickResetApp()
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
                    Text(
                        stringResource(Res.string.heading_label_settings_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("${stringResource(Res.string.text_label_stage)}: $stage")
                Text("${stringResource(Res.string.text_label_build)}: $version-$buildType")
            }
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val layoutSpacingModifier = Modifier.padding(top = 24.dp)
                Column {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_configuration),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    OutlinedTextField(
                        value = host,
                        onValueChange = onChangeHost,
                        label = {
                            Text(stringResource(Res.string.text_label_issuing_service))
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenuBox(
                        expanded = showCredentialRepresentationMenu,
                        onExpandedChange = {
                            showCredentialRepresentationMenu = !showCredentialRepresentationMenu
                        },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = credentialRepresentation.name,
                            onValueChange = {},
                            label = {
                                Text(stringResource(Res.string.text_label_id_format))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCredentialRepresentationMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = showCredentialRepresentationMenu,
                            onDismissRequest = {
                                showCredentialRepresentationMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.id_format_plain_jwt_label))
                                },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.PLAIN_JWT)
                                    showCredentialRepresentationMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.id_format_sd_jwt_label))
                                },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.SD_JWT)
                                    showCredentialRepresentationMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.id_format_iso_mdoc_label))
                                },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.ISO_MDOC)
                                    showCredentialRepresentationMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = showCredentialSchemeMenu,
                        onExpandedChange = {
                            showCredentialSchemeMenu = !showCredentialSchemeMenu
                        },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = credentialSchemeVcType,
                            onValueChange = {},
                            label = {
                                Text(stringResource(Res.string.text_label_id_scheme))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCredentialSchemeMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = showCredentialSchemeMenu,
                            onDismissRequest = {
                                showCredentialSchemeMenu = false
                            },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(IdAustriaScheme.vcType)
                                },
                                onClick = {
                                    onChangeCredentialSchemeVcType(IdAustriaScheme.vcType)
                                    showCredentialSchemeMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(EuPidScheme.vcType)
                                },
                                onClick = {
                                    onChangeCredentialSchemeVcType(EuPidScheme.vcType)
                                    showCredentialSchemeMenu = false
                                },
                            )
                        }
                    }
                    SaveButton(
                        onClick = onClickSaveConfiguration,
                        enabled = isSaveEnabled,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
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
                        onClick = onClickFAQs,
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
                        onClick = onClickDataProtectionPolicy,
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
                        onClick = onClickLicenses,
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
                }
            }
        }
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


@OptIn(ExperimentalResourceApi::class)
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