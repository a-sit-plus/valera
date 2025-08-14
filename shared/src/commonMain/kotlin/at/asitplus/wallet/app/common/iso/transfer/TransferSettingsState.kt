package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.multipaz.compose.permissions.PermissionState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState

@Stable
data class TransferSettingsState(
    val bleCentral: Boolean,
    val blePeripheral: Boolean,
    val nfcSetting: Boolean,

    val bleSettingOn: Boolean,
    val nfcSettingOn: Boolean,

    val bleRequired: Boolean,
    val nfcRequired: Boolean,
    val isBleEnabled: Boolean,
    val isNfcEnabled: Boolean,

    val blePermissionState: PermissionState,
    val missingRequiredBlePermission: Boolean,

    val transferMethodAvailableForCurrentSettings: Boolean,

    val isAnyTransferMethodSettingOn: Boolean,

    val isInitialized: Boolean
)

@Composable
fun rememberTransferSettingsState(
    settingsRepository: SettingsRepository
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

    val isBleEnabled = CapabilityManager.isBluetoothEnabled()
    val isNfcEnabled = CapabilityManager.isNfcEnabled()

    val missingRequiredBlePermission = bleRequired && !blePermissionState.isGranted

    val transferMethodAvailableForCurrentSettings =
        CapabilityManager.isTransferMethodAvailableForCurrentSettings(
            isBleSettingOn = bleSettingOn,
            isNfcSettingOn = nfcSettingOn
        )

    var isInitialized by remember(settingsRepository) { mutableStateOf(false) }
    LaunchedEffect(settingsRepository) {
        combine(
            settingsRepository.presentmentBleCentralClientModeEnabled,
            settingsRepository.presentmentBlePeripheralServerModeEnabled,
            settingsRepository.presentmentNfcDataTransferEnabled
        ) { _, _, _ -> }.first()
        isInitialized = true
    }

    return TransferSettingsState(
        bleCentral = bleCentral,
        blePeripheral = blePeripheral,
        nfcSetting = nfcSetting,
        bleSettingOn = bleSettingOn,
        nfcSettingOn = nfcSettingOn,
        bleRequired = bleRequired,
        nfcRequired = nfcRequired,
        isBleEnabled = isBleEnabled,
        isNfcEnabled = isNfcEnabled,
        blePermissionState = blePermissionState,
        missingRequiredBlePermission = missingRequiredBlePermission,
        transferMethodAvailableForCurrentSettings = transferMethodAvailableForCurrentSettings,
        isAnyTransferMethodSettingOn = isAnyTransferMethodSettingOn,
        isInitialized = isInitialized
    )
}
