package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBManagerStatePoweredOn

actual class BluetoothInfo {
    private val centralManager = CBCentralManager(null, null)
    @Composable
    actual fun isBluetoothEnabled(): Boolean {
        // TODO: check this implementation
        return centralManager.state == CBManagerStatePoweredOn
    }

    actual fun openBluetoothSettings(platformContext: PlatformContext) {
        // On iOS, there is no direct way to open Bluetooth settings
        // TODO: check this implementation
        openAppSettings(platformContext)
    }
}
