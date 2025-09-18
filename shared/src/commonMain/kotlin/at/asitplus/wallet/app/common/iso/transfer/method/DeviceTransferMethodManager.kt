package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Stable
import at.asitplus.wallet.app.common.PlatformAdapter

@Stable
class DeviceTransferMethodManager(
    val platformContext: PlatformContext,
    val platformAdapter: PlatformAdapter
) {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()
    private val appSettings = AppSettings()

    fun isBluetoothEnabled(): Boolean = bluetoothInfo.isBluetoothEnabled(platformContext)
    fun goToBluetoothSettings() =
        bluetoothInfo.openBluetoothSettings(platformContext, platformAdapter)

    fun isNfcEnabled(): Boolean = nfcInfo.isNfcEnabled(platformContext)
    fun goToNfcSettings() = nfcInfo.openNfcSettings(platformContext, platformAdapter)

    fun openAppSettings() = appSettings.open(platformContext, platformAdapter)
}
