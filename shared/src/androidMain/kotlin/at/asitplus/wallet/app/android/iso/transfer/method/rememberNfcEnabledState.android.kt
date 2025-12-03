package at.asitplus.wallet.app.common.iso.transfer.method

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class NfcEnabledState internal constructor(
    private val context: Context,
    private val adapter: NfcAdapter?
) {
    private val _isEnabled = mutableStateOf(adapter?.isEnabled == true)
    actual val isEnabled: Boolean get() = _isEnabled.value

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    NfcAdapter.EXTRA_ADAPTER_STATE,
                    NfcAdapter.STATE_OFF
                )
                _isEnabled.value = (state == NfcAdapter.STATE_ON)
            }
        }
    }

    init {
        context.registerReceiver(
            receiver,
            IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        )
    }

    actual suspend fun enable() {
        // NFC cannot be enabled programmatically — open settings instead
        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun unregister() {
        context.unregisterReceiver(receiver)
    }
}

@Composable
actual fun rememberNfcEnabledState(): NfcEnabledState {
    val context = LocalContext.current.applicationContext
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val state = remember { NfcEnabledState(context, adapter) }

    DisposableEffect(Unit) {
        onDispose { state.unregister() }
    }
    return state
}
