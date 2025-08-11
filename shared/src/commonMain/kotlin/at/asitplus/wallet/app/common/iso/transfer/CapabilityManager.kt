package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

object CapabilityManager {
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()

    @Composable
    fun isBluetoothEnabled(): Boolean = bluetoothInfo.isBluetoothEnabled()

    @Composable
    fun isNfcEnabled(): Boolean = nfcInfo.isNfcEnabled()

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
