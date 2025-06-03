package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

object CapabilityManager {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()

    @Composable
    fun isBluetoothEnabled(): Boolean {
        return bluetoothInfo.isBluetoothEnabled()
    }

    @Composable
    fun isNfcEnabled(): Boolean {
        return nfcInfo.isNfcEnabled()
    }

    @Composable
    fun isAnyTransferMethodAvailable(): Boolean {
        return (isBluetoothEnabled() || isNfcEnabled())
    }
}
