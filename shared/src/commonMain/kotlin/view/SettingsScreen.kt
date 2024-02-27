package view

import Resources
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.ConstantIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun SettingsScreen(
    navigateToLogPage: () -> Unit,
    onClickResetApp: () -> Unit,
    walletMain: WalletMain,
) {
    // TODO("get stage and version")
    val stage = "T"
    val version = "1.0.0 / 2389237"

    var credentialRepresentation by remember {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.credentialRepresentation.first())
        }
    }
    var host by rememberSaveable {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.host.first())
        }
    }
    var isSaveEnabled by rememberSaveable {
        mutableStateOf(false)
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
        onChangeIsSaveEnabled = {
            isSaveEnabled = it
        },
        onClickSaveConfiguration = {
            walletMain.walletConfig.set(
                host = host,
                credentialRepresentation = credentialRepresentation,
            )
        },
        stage = stage,
        version = version,
        onClickFAQs = {},
        onClickDataProtectionPolicy = {},
        onClickLicenses = {},
        onClickShareLogFile = navigateToLogPage,
        onClickResetApp = onClickResetApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsView(
    host: String,
    onChangeHost: (String) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    isSaveEnabled: Boolean,
    onChangeIsSaveEnabled: (Boolean) -> Unit,
    onClickSaveConfiguration: () -> Unit,
    stage: String,
    version: String,
    onClickFAQs: () -> Unit,
    onClickDataProtectionPolicy: () -> Unit,
    onClickLicenses: () -> Unit,
    onClickShareLogFile: () -> Unit,
    onClickResetApp: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val actualShowMenu = remember(showMenu, isSaveEnabled) {
        showMenu && isSaveEnabled
    }

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
                        "Einstellungen",
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
                Text("Stage: $stage")
                Text("Version: $version")
            }
        },
        floatingActionButton = {
            if (isSaveEnabled) {
                FloatingActionButton(
                    onClick = {
                        showMenu = false
                        onChangeIsSaveEnabled(false)
                        onClickSaveConfiguration()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = Resources.CONTENT_DESCRIPTION_SAVE_BUTTON
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = {
                        showMenu = false
                        onChangeIsSaveEnabled(true)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = Resources.CONTENT_DESCRIPTION_EDIT_BUTTON
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val layoutSpacingModifier = Modifier.padding(top = 24.dp)
                Column(
//                    modifier = layoutSpacingModifier // not for the first element
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = "Konfiguration",
                        style = MaterialTheme.typography.titleMedium,
//                        modifier = listSpacingModifier, // not for the first element
                    )

                    OutlinedTextField(
                        value = host,
                        onValueChange = onChangeHost,
                        label = { Text("Issuing Service") },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        enabled = isSaveEnabled,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenuBox(
                        expanded = actualShowMenu,
                        onExpandedChange = {
                            showMenu = !showMenu
                        },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = credentialRepresentation.name,
                            onValueChange = {},
                            label = { Text("Credential Representation") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu && isSaveEnabled) },
                            enabled = isSaveEnabled,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = actualShowMenu,
                            onDismissRequest = {
                                showMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            DropdownMenuItem(
                                text = { Text("PLAIN_JWT") },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.PLAIN_JWT)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenuItem(
                                text = { Text("SD_JWT") },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.SD_JWT)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenuItem(
                                text = { Text("ISO_MDOC") },
                                onClick = {
                                    onChangeCredentialRepresentation(ConstantIndex.CredentialRepresentation.ISO_MDOC)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.Center,
//                        modifier = Modifier.fillMaxWidth(),
//                    ) {
//                        Button(
//                            enabled = isSaveEnabled,
//                            onClick = onClickSaveConfiguration,
//                        ) {
//                            Text(Resources.BUTTON_LABEL_SAVE)
//                        }
//                    }
                }
                Column(
                    modifier = layoutSpacingModifier // not for the first element
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = "Informationen",
                        style = MaterialTheme.typography.titleMedium,
//                        modifier = listSpacingModifier, // not for the first element
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                            )
                        },
                        label = "FAQs",
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
                        label = "Datenschutz-Policy",
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
                        label = "Lizenzen",
                        onClick = onClickLicenses,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
                Column(
                    modifier = layoutSpacingModifier // not for the first element
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = "Aktionen",
                        style = MaterialTheme.typography.titleMedium,
//                        modifier = listSpacingModifier, // not for the first element
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                            )
                        },
                        label = "Log-Datei teilen",
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
                        label = "App zurücksetzen",
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


@Composable
private fun ResetAlert(
    onDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(Resources.WARNING)
        },
        text = {
            Text(Resources.RESET_APP_ALERT_TEXT)
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(Resources.BUTTON_CONFIRM)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(Resources.BUTTON_DISMISS)
            }
        }
    )
}