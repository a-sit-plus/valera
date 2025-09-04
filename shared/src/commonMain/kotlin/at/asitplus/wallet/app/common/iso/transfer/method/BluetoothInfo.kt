package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable

internal expect class BluetoothInfo() {
    @Composable
    fun isBluetoothEnabled(): Boolean
    fun openBluetoothSettings(platformContext: PlatformContext)
}
