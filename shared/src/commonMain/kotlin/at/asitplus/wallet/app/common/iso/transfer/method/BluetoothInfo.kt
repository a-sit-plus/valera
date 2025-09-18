package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.PlatformAdapter

internal expect class BluetoothInfo() {
    @Composable
    fun isBluetoothEnabled(): Boolean
    fun openBluetoothSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter)
}
