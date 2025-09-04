package at.asitplus.wallet.app.common.iso.transfer.capability

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
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

    actual fun openBluetoothSettings(platformContext: PlatformContext) {
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
