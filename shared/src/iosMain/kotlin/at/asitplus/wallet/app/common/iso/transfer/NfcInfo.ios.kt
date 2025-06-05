package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

actual class NfcInfo {
    @Composable
    actual fun isNfcEnabled(): Boolean {
        // TODO add check if nfc is enabled (if that's even possible on iOS)
        // for now return true so that NFC can be used if it's enabled
        return true
    }
}
