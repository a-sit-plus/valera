package at.asitplus.wallet.app.common.iso.transfer.method

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal actual class BluetoothInfo actual constructor(
    val platformContext: PlatformContext,
    platformAdapter: PlatformAdapter
) {
    private val _isBluetoothEnabled = MutableStateFlow(checkBluetoothEnabled())
    actual val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled

    private fun checkBluetoothEnabled(): Boolean {
        val manager = platformContext.context
            .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                _isBluetoothEnabled.value = checkBluetoothEnabled()
            }
        }
    }

    init {
        platformContext.context.registerReceiver(
            receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    actual fun openBluetoothSettings() {
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
