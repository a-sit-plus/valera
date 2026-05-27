package at.asitplus.wallet.app.common.presentation

import kotlinx.coroutines.flow.MutableStateFlow

enum class NfcDispatchSuppressionMode {
    NONE,
    REDISPATCH,
    DISABLED
}

object NfcTransferState {
    // True while NFC data transfer is the active transport for the current presentment.
    // The activity observes this to switch the preferred HCE service between the engagement
    // service (NdefDeviceEngagementService) and the data retrieval service (NfcDataRetrievalService).
    val nfcDataTransferActive = MutableStateFlow(false)

    // True while the verifier role is actively transferring data over NFC.
    // The activity observes this to keep NFC reader mode enabled, counteracting the 3-second
    // disableReaderMode() call that multipaz's ScanNfcTagPromptDialog schedules after the
    // engagement handover scan completes. Without this, the ongoing isoDep.transceive() would
    // block indefinitely (up to its 20-second timeout) when reader mode is disabled.
    val verifierNfcTransferActive = MutableStateFlow(false)

    // Controls short-lived NFC suppression after verifier transfer or while blocking UI is shown.
    // REDISPATCH claims reader mode with a no-op callback so Android does not redispatch a
    // still-present tag. DISABLED unsets foreground HCE and disables reader mode.
    val verifierNfcTagDispatchSuppressed = MutableStateFlow(NfcDispatchSuppressionMode.NONE)
}
