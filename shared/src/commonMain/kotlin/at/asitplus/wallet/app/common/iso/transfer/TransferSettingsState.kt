package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.wallet.app.common.data.SettingsRepository
import org.multipaz.compose.permissions.rememberBluetoothPermissionState

@Stable
data class TransferSettingsState(
    val bleSettingOn: Boolean,
    val nfcSettingOn: Boolean,
    val bleRequired: Boolean,
    val nfcRequired: Boolean,
    val isBleEnabled: Boolean,
    val isNfcEnabled: Boolean,
    val missingRequiredBlePermission: Boolean,
    val transferMethodAvailableForCurrentSettings: Boolean,
    val isAnyTransferMethodSettingOn: Boolean
)

@Composable
fun rememberTransferSettingsState(
    settingsRepository: SettingsRepository,
    capabilityManager: CapabilityManager
): TransferSettingsState {
    val blePermissionState = rememberBluetoothPermissionState()

    val bleCentral = settingsRepository.presentmentBleCentralClientModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val blePeripheral = settingsRepository.presentmentBlePeripheralServerModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val nfcSetting = settingsRepository.presentmentNfcDataTransferEnabled
        .collectAsStateWithLifecycle(initialValue = false).value

    val bleSettingOn = bleCentral || blePeripheral
    val nfcSettingOn = nfcSetting
    val isAnyTransferMethodSettingOn = bleSettingOn || nfcSettingOn

    val bleRequired = bleSettingOn && !nfcSettingOn
    val nfcRequired = nfcSettingOn && !bleSettingOn

    val isBleEnabled = capabilityManager.isBluetoothEnabled()
    val isNfcEnabled = capabilityManager.isNfcEnabled()

    val missingRequiredBlePermission = bleRequired && !blePermissionState.isGranted
    val transferMethodAvailableForCurrentSettings =
        capabilityManager.isTransferMethodAvailableForCurrentSettings(
            isBleSettingOn = bleSettingOn,
            isNfcSettingOn = nfcSettingOn
        )

    return TransferSettingsState(
        bleSettingOn,
        nfcSettingOn,
        bleRequired,
        nfcRequired,
        isBleEnabled,
        isNfcEnabled,
        missingRequiredBlePermission,
        transferMethodAvailableForCurrentSettings,
        isAnyTransferMethodSettingOn
    )
}
