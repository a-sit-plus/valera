package ui.views

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.runBlocking
import navigation.Page

class InformationPage : Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationView(
    stage: String,
    version: String,
    onClickFAQs: () -> Unit,
    onClickDataProtectionPolicy: () -> Unit,
    onClickLicenses: () -> Unit,
    onClickShareLogFile: () -> Unit,
    onClickResetApp: () -> Unit,
) {
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
                        "Informationen",
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
        }
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