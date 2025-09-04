package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable

expect class BluetoothInfo() {
    @Composable
    fun isBluetoothEnabled(): Boolean
    fun openBluetoothSettings(platformContext: PlatformContext)
}
