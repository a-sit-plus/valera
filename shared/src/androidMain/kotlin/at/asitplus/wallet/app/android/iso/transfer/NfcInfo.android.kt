package at.asitplus.wallet.app.common.iso.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


actual class NfcInfo {
    @Composable
    actual fun isNfcEnabled(): Boolean {
        val context = LocalContext.current
        val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
        val initialState = nfcAdapter?.isEnabled == true
        val state = remember { mutableStateOf(initialState) }

        DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                        val newState = nfcAdapter?.isEnabled == true
                        state.value = newState
                    }
                }
            }

            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            context.registerReceiver(receiver, filter)

            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        return state.value
    }
}
