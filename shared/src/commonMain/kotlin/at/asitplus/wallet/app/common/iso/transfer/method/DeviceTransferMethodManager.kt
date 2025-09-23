package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Stable
import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.coroutines.flow.StateFlow

@Stable
class DeviceTransferMethodManager(
    val platformContext: PlatformContext,
    val platformAdapter: PlatformAdapter
) {
    private val bluetoothInfo = BluetoothInfo(platformContext, platformAdapter)
    private val nfcInfo = NfcInfo(platformContext, platformAdapter)
    private val appSettings = AppSettings(platformContext, platformAdapter)

    val  isBluetoothEnabled: StateFlow<Boolean> = bluetoothInfo.isBluetoothEnabled
    fun goToBluetoothSettings() = bluetoothInfo.openBluetoothSettings()

    val isNfcEnabled: StateFlow<Boolean> = nfcInfo.isNfcEnabled
    fun goToNfcSettings() = nfcInfo.openNfcSettings()

    fun openAppSettings() = appSettings.open()
}
