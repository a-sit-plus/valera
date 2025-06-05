package at.asitplus.wallet.app.common.iso.transfer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class BluetoothInfo {
    @Composable
    actual fun isBluetoothEnabled(): Boolean {
        val context = LocalContext.current
        val bluetoothManager = remember {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        }
        val isBluetoothEnabled = remember {
            mutableStateOf(bluetoothManager?.adapter?.isEnabled == true)
        }

        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        isBluetoothEnabled.value = bluetoothManager?.adapter?.isEnabled == true
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            onDispose { context.unregisterReceiver(receiver) }
        }
        return isBluetoothEnabled.value
    }
}
