@file:OptIn(ExperimentalResourceApi::class)

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
import androidx.compose.material.icons.outlined.HelpOutline
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
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.BUTTON_LABEL_CONFIRM
import composewalletapp.shared.generated.resources.BUTTON_LABEL_DATA_PROTECTION_POLICY
import composewalletapp.shared.generated.resources.BUTTON_LABEL_DISMISS
import composewalletapp.shared.generated.resources.BUTTON_LABEL_FAQ
import composewalletapp.shared.generated.resources.BUTTON_LABEL_LICENSES
import composewalletapp.shared.generated.resources.BUTTON_LABEL_RESET_APP
import composewalletapp.shared.generated.resources.BUTTON_LABEL_SHARE_LOG_FILE
import composewalletapp.shared.generated.resources.ERROR_FEATURE_NOT_YET_AVAILABLE
import composewalletapp.shared.generated.resources.HEADING_LABEL_SETTINGS_SCREEN
import composewalletapp.shared.generated.resources.ID_FORMAT_ISO_MDOC_LABEL
import composewalletapp.shared.generated.resources.ID_FORMAT_PLAIN_JWT_LABEL
import composewalletapp.shared.generated.resources.ID_FORMAT_SD_JWT_LABEL
import composewalletapp.shared.generated.resources.RESET_APP_ALERT_TEXT
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.SECTION_HEADING_ACTIONS
import composewalletapp.shared.generated.resources.SECTION_HEADING_CONFIGURATION
import composewalletapp.shared.generated.resources.SECTION_HEADING_INFORMATION
import composewalletapp.shared.generated.resources.TEXT_LABEL_BUILD
import composewalletapp.shared.generated.resources.TEXT_LABEL_ID_FORMAT
import composewalletapp.shared.generated.resources.TEXT_LABEL_ISSUING_SERVICE
import composewalletapp.shared.generated.resources.TEXT_LABEL_STAGE
import composewalletapp.shared.generated.resources.WARNING
import kotlinx.coroutines.flow.first
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

    val originalCredentialRepresentation by walletMain.walletConfig.credentialRepresentation.collectAsState(null)
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

    val isSaveEnabled = remember(originalHost, host, originalCredentialRepresentation, credentialRepresentation) {
        host != originalHost || credentialRepresentation != originalCredentialRepresentation
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
        isSaveEnabled = isSaveEnabled,
        onClickSaveConfiguration = {
            walletMain.walletConfig.set(
                host = host,
                credentialRepresentation = credentialRepresentation,
            )
        },
        buildType = buildType,
        stage = stage,
        version = version,
        onClickFAQs = {
            runBlocking {
                walletMain.snackbarService.showSnackbar(getString(Res.string.ERROR_FEATURE_NOT_YET_AVAILABLE))
            }
        },
        onClickDataProtectionPolicy = {
            runBlocking {
                walletMain.snackbarService.showSnackbar(getString(Res.string.ERROR_FEATURE_NOT_YET_AVAILABLE))
            }
        },
        onClickLicenses = {
            runBlocking {
                walletMain.snackbarService.showSnackbar(getString(Res.string.ERROR_FEATURE_NOT_YET_AVAILABLE))
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
    var showMenu by remember { mutableStateOf(false) }

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
                        stringResource(Res.string.HEADING_LABEL_SETTINGS_SCREEN),
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
                Text("${stringResource(Res.string.TEXT_LABEL_STAGE)}: $stage")
                Text("${stringResource(Res.string.TEXT_LABEL_BUILD)}: $version-$buildType")
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
                        text = stringResource(Res.string.SECTION_HEADING_CONFIGURATION),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    OutlinedTextField(
                        value = host,
                        onValueChange = onChangeHost,
                        label = {
                            Text(stringResource(Res.string.TEXT_LABEL_ISSUING_SERVICE))
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenuBox(
                        expanded = showMenu,
                        onExpandedChange = {
                            showMenu = !showMenu
                        },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = credentialRepresentation.name,
                            onValueChange = {},
                            label = {
                                Text(stringResource(Res.string.TEXT_LABEL_ID_FORMAT))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.ID_FORMAT_PLAIN_JWT_LABEL))
                                },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.PLAIN_JWT)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.ID_FORMAT_SD_JWT_LABEL))
                                },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.SD_JWT)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.ID_FORMAT_ISO_MDOC_LABEL))
                                },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.ISO_MDOC)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                        text = stringResource(Res.string.SECTION_HEADING_INFORMATION),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.BUTTON_LABEL_FAQ),
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
                        label = stringResource(Res.string.BUTTON_LABEL_DATA_PROTECTION_POLICY),
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
                        label = stringResource(Res.string.BUTTON_LABEL_LICENSES),
                        onClick = onClickLicenses,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
                Column(
                    modifier = layoutSpacingModifier
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.SECTION_HEADING_ACTIONS),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.BUTTON_LABEL_SHARE_LOG_FILE),
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
                        label = stringResource(Res.string.BUTTON_LABEL_RESET_APP),
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
            Text(stringResource(Res.string.WARNING))
        },
        text = {
            Text(stringResource(Res.string.RESET_APP_ALERT_TEXT))
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(stringResource(Res.string.BUTTON_LABEL_CONFIRM))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.BUTTON_LABEL_DISMISS))
            }
        }
    )
}