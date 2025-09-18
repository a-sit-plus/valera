package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.PlatformAdapter
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBManagerStatePoweredOn

actual class BluetoothInfo {
    private val centralManager = CBCentralManager(null, null)
    @Composable
    actual fun isBluetoothEnabled(): Boolean {
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
