package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

actual class BluetoothInfo {
    @Composable
    actual fun isBluetoothEnabled(): Boolean {
        return false
    }
}
