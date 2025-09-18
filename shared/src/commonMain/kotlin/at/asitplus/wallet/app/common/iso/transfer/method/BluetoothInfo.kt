package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter

internal expect class BluetoothInfo() {
    fun isBluetoothEnabled(platformContext: PlatformContext): Boolean
    fun openBluetoothSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter)
}
