package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.PlatformAdapter
import platform.CoreNFC.NFCNDEFReaderSession

actual class NfcInfo {
    @Composable
    actual fun isNfcEnabled(): Boolean {
        return NFCNDEFReaderSession.readingAvailable
    }

    actual fun openNfcSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter) {
        // TODO: check this implementation
        platformAdapter.openUrl("prefs:root=General")
    }
}
