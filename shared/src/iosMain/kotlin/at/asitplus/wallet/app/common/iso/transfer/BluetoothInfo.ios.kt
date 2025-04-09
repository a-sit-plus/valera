package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

actual class BluetoothInfo {
    @Composable
    actual fun isBluetoothEnabled(): Boolean {
        // TODO add check if bluetooth is enabled (if that's even possible on iOS
        // for now return true so that Bluetooth can be used if it's enabled
        return true
    }
}
