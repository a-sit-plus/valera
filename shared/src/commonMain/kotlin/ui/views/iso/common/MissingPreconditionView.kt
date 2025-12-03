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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_missing_precondition
import at.asitplus.wallet.app.common.iso.transfer.method.NfcEnabledState
import at.asitplus.wallet.app.common.iso.transfer.state.PreconditionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.permissions.BluetoothEnabledState
import org.multipaz.compose.permissions.PermissionState
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingPreconditionView(
    reason: PreconditionState,
    bluetoothPermissionState: PermissionState,
    bluetoothEnabledState: BluetoothEnabledState,
    nfcEnabledState: NfcEnabledState,
    onClickSettings: () -> Unit,
    navigateUp: (() -> Unit),
    onClickLogo: (() -> Unit),
    onClickBackToSettings: (() -> Unit),
    onOpenAppSettings: (() -> Unit)
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
                when (reason) {
                    PreconditionState.NO_TRANSFER_METHOD_SELECTED ->
                        NoTransferMethodSelectedView(onClickBackToSettings = onClickBackToSettings)
                    PreconditionState.BLE_SELECTED_BUT_NOT_ENABLED ->
                        BleSelectedButNotEnabledView(
                            onClickBackToSettings = onClickBackToSettings,
                            onEnableBluetooth = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    bluetoothEnabledState.enable()
                                }
                            }
                        )
                    PreconditionState.NFC_SELECTED_BUT_NOT_ENABLED ->
                        NfcSelectedButNotEnabledView(
                            onClickBackToSettings = onClickBackToSettings,
                            onOpenDeviceSettings = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    nfcEnabledState.enable()
                                }
                            }
                        )
                    PreconditionState.MISSING_PERMISSION -> {
                        LaunchedEffect(reason) {
                            bluetoothPermissionState.launchPermissionRequest()
                        }
                        MissingBluetoothPermissionView(
                            onOpenAppPermissionSettings = onOpenAppSettings
                        )
                    }
                    PreconditionState.NFC_ENGAGEMENT_NOT_AVAILABLE ->
                        NfcEngagementNotAvailableView(
                            onClickBackToSettings = onClickBackToSettings,
                            onOpenDeviceSettings = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    nfcEnabledState.enable()
                                }
                            }
                        )
                    PreconditionState.OK -> { } // This should not happen
                }
            }
        }
    }
}
