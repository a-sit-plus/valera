package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

class CapabilityManager {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()

    @Composable
    fun isBluetoothEnabled(): Boolean = bluetoothInfo.isBluetoothEnabled()

    fun goToBluetoothSettings(platformContext: PlatformContext) = bluetoothInfo.openSettings(platformContext)

    @Composable
    fun isNfcEnabled(): Boolean = nfcInfo.isNfcEnabled()

    fun goToNfcSettings(platformContext: PlatformContext) = nfcInfo.openSettings(platformContext)

    @Composable
    fun isAnyTransferMethodAvailable(): Boolean = isBluetoothEnabled() || isNfcEnabled()

    @Composable
    fun isTransferMethodAvailableForCurrentSettings(
        isBleSettingOn: Boolean,
        isNfcSettingOn: Boolean
    ): Boolean {
        return (isBleSettingOn && isBluetoothEnabled()) || (isNfcSettingOn && isNfcEnabled())
    }
}
