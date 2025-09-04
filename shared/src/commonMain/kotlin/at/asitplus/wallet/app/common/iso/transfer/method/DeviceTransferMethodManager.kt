package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class DeviceTransferMethodManager {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()
    private val appSettings = AppSettings()

    @Composable
    fun isBluetoothEnabled(): Boolean = bluetoothInfo.isBluetoothEnabled()
    fun goToBluetoothSettings(platformContext: PlatformContext) =
        bluetoothInfo.openBluetoothSettings(platformContext)

    @Composable
    fun isNfcEnabled(): Boolean = nfcInfo.isNfcEnabled()
    fun goToNfcSettings(platformContext: PlatformContext) = nfcInfo.openNfcSettings(platformContext)

    fun openAppSettings(platformContext: PlatformContext) = appSettings.open(platformContext)
}
