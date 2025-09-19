package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.darwin.NSObject

internal actual class BluetoothInfo actual constructor(
    platformContext: PlatformContext,
    val platformAdapter: PlatformAdapter
) : NSObject(), CBCentralManagerDelegateProtocol {
    private val _isBluetoothEnabled = MutableStateFlow(false)
    actual val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled

    private var centralManager: CBCentralManager? = null

    init {
        centralManager = CBCentralManager(delegate = this, queue = null)
        _isBluetoothEnabled.value = centralManager?.state == CBManagerStatePoweredOn
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        _isBluetoothEnabled.value = central.state == CBManagerStatePoweredOn
    }

    actual fun openBluetoothSettings() {
        platformAdapter.openUrl("App-prefs:")
    }
}
