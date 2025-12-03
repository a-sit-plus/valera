package at.asitplus.wallet.app.common.iso.transfer.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.wallet.app.common.data.SettingsRepository

@Stable
data class ChannelState(
    val settingOn: Boolean,
    val required: Boolean,
)

@Stable
data class TransferSettingsState(
    val ble: ChannelState,
    val nfc: ChannelState
) {
    val isAnySettingOn: Boolean get() = ble.settingOn || nfc.settingOn
}

@Composable
fun rememberTransferSettingsState(settingsRepository: SettingsRepository): TransferSettingsState {
    val bleCentralClientModeEnabled = settingsRepository.presentmentBleCentralClientModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val blePeripheralServerModeEnabled = settingsRepository.presentmentBlePeripheralServerModeEnabled
        .collectAsStateWithLifecycle(initialValue = false).value
    val nfcSettingOn = settingsRepository.presentmentNfcDataTransferEnabled
        .collectAsStateWithLifecycle(initialValue = false).value

    return buildTransferSettingsState(
        bleCentralClientModeEnabled,
        blePeripheralServerModeEnabled,
        nfcSettingOn
    )
}

fun buildTransferSettingsState(
    bleCentralClientModeEnabled: Boolean,
    blePeripheralServerModeEnabled: Boolean,
    nfcSettingOn: Boolean
): TransferSettingsState {
    val bleSettingOn = bleCentralClientModeEnabled || blePeripheralServerModeEnabled
    return TransferSettingsState(
        ble = ChannelState(
            settingOn = bleSettingOn,
            required = bleSettingOn && !nfcSettingOn
        ),
        nfc = ChannelState(
            settingOn = nfcSettingOn,
            required = nfcSettingOn && !bleSettingOn
        )
    )
}
