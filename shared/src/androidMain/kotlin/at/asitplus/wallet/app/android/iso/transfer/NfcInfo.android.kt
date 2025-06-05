package at.asitplus.wallet.app.common.iso.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.nfc.NfcAdapter
import androidx.compose.ui.platform.LocalContext

actual class NfcInfo {
    @Composable
    actual fun isNfcEnabled(): Boolean {
        val context = LocalContext.current
        val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
        val isNfcEnabled = remember { mutableStateOf(nfcAdapter?.isEnabled == true) }

        DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                        val currentAdapter = NfcAdapter.getDefaultAdapter(context)
                        isNfcEnabled.value = currentAdapter?.isEnabled == true
                    }
                }
            }
            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            context.registerReceiver(receiver, filter)
            onDispose { context.unregisterReceiver(receiver) }
        }
        return isNfcEnabled.value
    }
}
