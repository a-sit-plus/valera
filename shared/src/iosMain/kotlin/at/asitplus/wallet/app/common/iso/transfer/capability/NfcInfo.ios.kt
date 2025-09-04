package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable
import platform.CoreNFC.NFCNDEFReaderSession

actual class NfcInfo {
    @Composable
    actual fun isNfcEnabled(): Boolean {
        // TODO: check this implementation
        return NFCNDEFReaderSession.readingAvailable
    }

    actual fun openNfcSettings(platformContext: PlatformContext) {
        // On iOS, there is no direct way to open Nfc settings
        // TODO: check this implementation
        openAppSettings(platformContext)
    }
}
