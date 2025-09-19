package ui.views.iso.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_missing_precondition
import at.asitplus.wallet.app.common.iso.transfer.method.BluetoothEnabledState
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceTransferMethodManager
import at.asitplus.wallet.app.common.iso.transfer.state.PreconditionState
import at.asitplus.wallet.app.common.iso.transfer.state.TransferSettingsState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.permissions.PermissionState
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingPreconditionView(
    reason: PreconditionState,
    transferSettingsState: TransferSettingsState,
    deviceTransferMethodManager: DeviceTransferMethodManager,
    bluetoothPermissionState: PermissionState,
    bluetoothEnabledState: BluetoothEnabledState,
    onClickSettings: () -> Unit,
    navigateUp: (() -> Unit),
    onClickLogo: (() -> Unit)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_missing_precondition),
                            Modifier.weight(1f),
                        )
                    }
                },
                navigationIcon = { NavigateUpButton(navigateUp) },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                MissingPreconditionViewBody(
                    reason = reason,
                    transferSettingsState = transferSettingsState,
                    deviceTransferMethodManager = deviceTransferMethodManager,
                    bluetoothPermissionState = bluetoothPermissionState,
                    bluetoothEnabledState = bluetoothEnabledState,
                    onClickBackToSettings = navigateUp,
                )
            }
        }
    }
}

@Composable
fun MissingPreconditionViewBody(
    reason: PreconditionState,
    transferSettingsState: TransferSettingsState,
    deviceTransferMethodManager: DeviceTransferMethodManager,
    bluetoothPermissionState: PermissionState,
    bluetoothEnabledState: BluetoothEnabledState,
    onClickBackToSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    when (reason) {
        PreconditionState.NO_TRANSFER_METHOD_SELECTED ->
            NoTransferMethodSelectedView(onClickBackToSettings = onClickBackToSettings)

        PreconditionState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION ->
            NoTransferMethodAvailableView(
                onClickBackToSettings = onClickBackToSettings,
                onOpenDeviceSettings = {
                    if (transferSettingsState.nfc.required) {
                        deviceTransferMethodManager.goToNfcSettings()
                    } else {
                        coroutineScope.launch {
                            bluetoothEnabledState.enable()
                        }
                    }
                }
            )

        PreconditionState.MISSING_PERMISSION -> {
            LaunchedEffect(reason) {
                bluetoothPermissionState.launchPermissionRequest()
            }
            MissingBluetoothPermissionView(
                onOpenAppPermissionSettings = {
                    deviceTransferMethodManager.openAppSettings()
                }
            )
        }
    }
}
