package at.asitplus.wallet.app.common.iso.transfer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

actual class BluetoothInfo {
    @Composable
    actual fun isBluetoothEnabled(): Boolean {
        val context = LocalContext.current
        val bluetoothManager = remember {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        }
        val adapter = bluetoothManager?.adapter
        val initialState = adapter?.isEnabled == true
        val state = remember { mutableStateOf(initialState) }

        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        val newState = adapter?.isEnabled == true
                        state.value = newState
                    }
                }
            }

            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(receiver, filter)

            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        return state.value
    }
}
