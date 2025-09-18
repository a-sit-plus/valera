package at.asitplus.wallet.app.common.iso.transfer.method

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import at.asitplus.wallet.app.common.PlatformAdapter

actual class NfcInfo {
    actual fun isNfcEnabled(platformContext: PlatformContext): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(platformContext.context)
        return nfcAdapter?.isEnabled == true
    }

    actual fun openNfcSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter) {
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
