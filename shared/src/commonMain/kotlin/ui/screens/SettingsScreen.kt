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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.WalletMain
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_confirm
import compose_wallet_app.shared.generated.resources.button_label_data_protection_policy
import compose_wallet_app.shared.generated.resources.button_label_dismiss
import compose_wallet_app.shared.generated.resources.button_label_faq
import compose_wallet_app.shared.generated.resources.button_label_licenses
import compose_wallet_app.shared.generated.resources.button_label_reset_app
import compose_wallet_app.shared.generated.resources.button_label_share_log_file
import compose_wallet_app.shared.generated.resources.error_feature_not_yet_available
import compose_wallet_app.shared.generated.resources.heading_label_settings_screen
import compose_wallet_app.shared.generated.resources.reset_app_alert_text
import compose_wallet_app.shared.generated.resources.section_heading_actions
import compose_wallet_app.shared.generated.resources.section_heading_information
import compose_wallet_app.shared.generated.resources.text_label_build
import compose_wallet_app.shared.generated.resources.text_label_stage
import compose_wallet_app.shared.generated.resources.warning
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SettingsScreen(
    navigateToLogPage: () -> Unit,
    onClickResetApp: () -> Unit,
    onClickClearLog: () -> Unit,
    walletMain: WalletMain,
) {
    val stage = Configuration.BUILD_FOR_STAGE
    val buildType = walletMain.buildContext.buildType
    val version = walletMain.buildContext.versionName

    SettingsView(
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
        onClickClearLogFile = onClickClearLog,
        onClickResetApp = onClickResetApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun SettingsView(
    stage: String,
    version: String,
    buildType: String,
    onClickFAQs: () -> Unit,
    onClickDataProtectionPolicy: () -> Unit,
    onClickLicenses: () -> Unit,
    onClickShareLogFile: () -> Unit,
    onClickClearLogFile: () -> Unit,
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
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_clear_log),
                        onClick = onClickClearLogFile,
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