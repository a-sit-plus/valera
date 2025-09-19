package at.asitplus.wallet.app.common.iso.transfer.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceTransferMethodManager
import at.asitplus.wallet.app.common.iso.transfer.method.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState

sealed interface TransferPrecondition {
    data object Ok : TransferPrecondition
    data object NoTransferMethodSelected : TransferPrecondition
    data object NoTransferMethodAvailable : TransferPrecondition
    data object MissingPermission : TransferPrecondition
}

@Stable
data class ChannelState(
    val settingOn: Boolean,
    val enabled: Boolean,
    val required: Boolean,
    val permissionGranted: Boolean = true
)

@Stable
data class TransferSettingsState(
    val ble: ChannelState,
    val nfc: ChannelState,
) {
    val isAnySettingOn: Boolean get() = ble.settingOn || nfc.settingOn

    val transferMethodAvailable: Boolean get() =
        (ble.settingOn && ble.enabled) || (nfc.settingOn && nfc.enabled)

    val precondition: TransferPrecondition
        get() = when {
            !isAnySettingOn -> TransferPrecondition.NoTransferMethodSelected
            !transferMethodAvailable -> TransferPrecondition.NoTransferMethodAvailable
            ble.required && !ble.permissionGranted -> TransferPrecondition.MissingPermission
            else -> TransferPrecondition.Ok
        }
}

@Composable
fun rememberTransferSettingsState(
    settingsRepository: SettingsRepository,
    deviceTransferMethodManager: DeviceTransferMethodManager
): TransferSettingsState {
    val bleCentralClientModeEnabled = settingsRepository.presentmentBleCentralClientModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val blePeripheralServerModeEnabled = settingsRepository.presentmentBlePeripheralServerModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val nfcSettingOn = settingsRepository.presentmentNfcDataTransferEnabled
        .collectAsStateWithLifecycle(initialValue = false).value

    val bleSettingOn = bleCentralClientModeEnabled || blePeripheralServerModeEnabled

    val bluetoothPermissionState = rememberBluetoothPermissionState()
    val bluetoothEnabledState = rememberBluetoothEnabledState()
    val isNfcEnabled = deviceTransferMethodManager.isNfcEnabled.collectAsState()

    val ble = ChannelState(
        settingOn = bleSettingOn,
        enabled = bluetoothEnabledState.isEnabled,
        required = bleSettingOn && !nfcSettingOn,
        permissionGranted = bluetoothPermissionState.isGranted
    )

    val nfc = ChannelState(
        settingOn = nfcSettingOn,
        required = nfcSettingOn && !bleSettingOn,
        enabled = isNfcEnabled.value
    )

    return TransferSettingsState(ble, nfc)
}
