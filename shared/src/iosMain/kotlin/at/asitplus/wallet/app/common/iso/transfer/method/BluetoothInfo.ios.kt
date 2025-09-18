package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBManagerStatePoweredOn

actual class BluetoothInfo {
    private val centralManager = CBCentralManager(null, null)
    actual fun isBluetoothEnabled(platformContext: PlatformContext): Boolean {
        return centralManager.state == CBManagerStatePoweredOn
    }

    actual fun openBluetoothSettings(
        platformContext: PlatformContext,
        platformAdapter: PlatformAdapter
    ) {
        // TODO: check this implementation
        platformAdapter.openUrl("App-prefs:")
    }
}
