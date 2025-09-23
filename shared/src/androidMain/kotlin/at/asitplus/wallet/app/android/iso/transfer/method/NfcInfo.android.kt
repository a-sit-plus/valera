package at.asitplus.wallet.app.common.iso.transfer.method

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.provider.Settings
import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class NfcInfo actual constructor(
    private val platformContext: PlatformContext,
    private val platformAdapter: PlatformAdapter
) {
    private val _isNfcEnabled = MutableStateFlow(checkNfcEnabled())
    actual val isNfcEnabled: StateFlow<Boolean> = _isNfcEnabled

    private fun checkNfcEnabled(): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(platformContext.context)
        return adapter?.isEnabled == true
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                _isNfcEnabled.value = checkNfcEnabled()
            }
        }
    }

    init {
        platformContext.context.registerReceiver(
            receiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        )
    }

    actual fun openNfcSettings() {
        runCatching {
            platformContext.context.startActivity(
                Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            platformContext.context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
