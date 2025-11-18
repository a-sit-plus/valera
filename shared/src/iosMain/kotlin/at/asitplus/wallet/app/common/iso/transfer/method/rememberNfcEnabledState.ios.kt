package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable

actual class NfcEnabledState {

    actual val isEnabled: Boolean
        get() = true // iOS doesn't provide a way to check global NFC state

    actual suspend fun enable() {
        // No-op: iOS does not allow programmatic NFC enabling
        // Could potentially open app settings if needed
    }
}

@Composable
actual fun rememberNfcEnabledState(): NfcEnabledState = NfcEnabledState()
