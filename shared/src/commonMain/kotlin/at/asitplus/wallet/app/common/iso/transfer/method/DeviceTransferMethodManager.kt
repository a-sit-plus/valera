package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import at.asitplus.wallet.app.common.PlatformAdapter

@Stable
class DeviceTransferMethodManager {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()
    private val appSettings = AppSettings()

    @Composable
    fun isBluetoothEnabled(): Boolean = bluetoothInfo.isBluetoothEnabled()
    fun goToBluetoothSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter) =
        bluetoothInfo.openBluetoothSettings(platformContext, platformAdapter)

    @Composable
    fun isNfcEnabled(): Boolean = nfcInfo.isNfcEnabled()
    fun goToNfcSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter) =
        nfcInfo.openNfcSettings(platformContext, platformAdapter)

    fun openAppSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter) =
        appSettings.open(platformContext, platformAdapter)
}
