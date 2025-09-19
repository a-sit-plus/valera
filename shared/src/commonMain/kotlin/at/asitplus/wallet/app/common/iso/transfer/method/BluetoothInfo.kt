package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.coroutines.flow.StateFlow

internal expect class BluetoothInfo(
    platformContext: PlatformContext,
    platformAdapter: PlatformAdapter
) {
    val isBluetoothEnabled: StateFlow<Boolean>
    fun openBluetoothSettings()
}
