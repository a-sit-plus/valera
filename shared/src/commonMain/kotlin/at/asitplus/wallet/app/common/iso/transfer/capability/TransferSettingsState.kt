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
data class TransferSettingsState(
    val bleSettingOn: Boolean,
    val nfcSettingOn: Boolean,
    val bleRequired: Boolean,
    val nfcRequired: Boolean,
    val isBleEnabled: Boolean,
    val isNfcEnabled: Boolean,
    val transferMethodAvailableForCurrentSettings: Boolean,
    val isAnyTransferMethodSettingOn: Boolean,
    val missingRequiredBlePermission: Boolean,
) {
    val precondition: TransferPrecondition
        get() = when {
            !isAnyTransferMethodSettingOn -> TransferPrecondition.NoTransferMethodSelected
            !transferMethodAvailableForCurrentSettings -> TransferPrecondition.NoTransferMethodAvailable
            missingRequiredBlePermission -> TransferPrecondition.MissingPermission
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
    val nfcDataTransferEnabled = settingsRepository.presentmentNfcDataTransferEnabled
        .collectAsStateWithLifecycle(initialValue = false).value

    val bleSettingOn = bleCentralClientModeEnabled || blePeripheralServerModeEnabled
    val nfcSettingOn = nfcDataTransferEnabled

    val bleRequired = bleSettingOn && !nfcSettingOn
    val nfcRequired = nfcSettingOn && !bleSettingOn

    val isBleEnabled = capabilityManager.isBluetoothEnabled()
    val isNfcEnabled = capabilityManager.isNfcEnabled()

    val transferMethodAvailableForCurrentSettings =
        capabilityManager.isTransferMethodAvailableForCurrentSettings(
            isBleSettingOn = bleSettingOn,
            isNfcSettingOn = nfcSettingOn
        )
    val isAnyTransferMethodSettingOn = bleSettingOn || nfcSettingOn

    val missingRequiredBlePermission = bleRequired && !blePermissionState.isGranted

    return TransferSettingsState(
        bleSettingOn,
        nfcSettingOn,
        bleRequired,
        nfcRequired,
        isBleEnabled,
        isNfcEnabled,
        transferMethodAvailableForCurrentSettings,
        isAnyTransferMethodSettingOn,
        missingRequiredBlePermission
    )
}
