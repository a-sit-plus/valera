package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

expect class BluetoothInfo() {
    @Composable
    fun isBluetoothEnabled(): Boolean
}
