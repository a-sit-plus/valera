package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.wallet.app.common.data.SettingsRepository
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
            ble.settingOn && !ble.permissionGranted -> TransferPrecondition.MissingPermission
            else -> TransferPrecondition.Ok
        }
}

@Composable
fun rememberTransferSettingsState(
    settingsRepository: SettingsRepository,
    capabilityManager: CapabilityManager
): TransferSettingsState {
    val blePermissionState = rememberBluetoothPermissionState()

    val bleCentralClientModeEnabled = settingsRepository.presentmentBleCentralClientModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val blePeripheralServerModeEnabled = settingsRepository.presentmentBlePeripheralServerModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val nfcSettingOn = settingsRepository.presentmentNfcDataTransferEnabled
        .collectAsStateWithLifecycle(initialValue = false).value

    val bleSettingOn = bleCentralClientModeEnabled || blePeripheralServerModeEnabled

    val ble = ChannelState(
        settingOn = bleSettingOn,
        enabled = capabilityManager.isBluetoothEnabled(),
        required = bleSettingOn && !nfcSettingOn,
        permissionGranted = blePermissionState.isGranted
    )

    val nfc = ChannelState(
        settingOn = nfcSettingOn,
        required = nfcSettingOn && !bleSettingOn,
        enabled = capabilityManager.isNfcEnabled()
    )

    return TransferSettingsState(ble, nfc)
}
