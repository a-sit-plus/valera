package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreNFC.NFCNDEFReaderSession

internal actual class NfcInfo actual constructor(
    platformContext: PlatformContext,
    val platformAdapter: PlatformAdapter
) {
    private val _isNfcEnabled = MutableStateFlow(false)
    actual val isNfcEnabled: StateFlow<Boolean> = _isNfcEnabled

    init {
        _isNfcEnabled.value = NFCNDEFReaderSession.readingAvailable
    }

    actual fun openNfcSettings() {
        Napier.w("NFC unavailable: This should not happen on iOS", tag = "NfcInfo")
        platformAdapter.openUrl("prefs:root=General")
    }
}
