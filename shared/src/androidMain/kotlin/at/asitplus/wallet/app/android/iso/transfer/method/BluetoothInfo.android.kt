package at.asitplus.wallet.app.common.iso.transfer.method

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import at.asitplus.wallet.app.common.PlatformAdapter

actual class BluetoothInfo {
    actual fun isBluetoothEnabled(platformContext: PlatformContext): Boolean {
        val bluetoothManager = platformContext.context
            .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    actual fun openBluetoothSettings(
        platformContext: PlatformContext,
        platformAdapter: PlatformAdapter
    ) {
        runCatching {
            platformContext.context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            platformContext.context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
