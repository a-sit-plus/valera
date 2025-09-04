package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class DeviceTransferMethodManager {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()

    @Composable
    fun isBluetoothEnabled(): Boolean = bluetoothInfo.isBluetoothEnabled()
    fun goToBluetoothSettings(platformContext: PlatformContext) = bluetoothInfo.openSettings(platformContext)

    @Composable
    fun isNfcEnabled(): Boolean = nfcInfo.isNfcEnabled()
    fun goToNfcSettings(platformContext: PlatformContext) = nfcInfo.openSettings(platformContext)
}
