package at.asitplus.wallet.app.common.presentation

import kotlinx.coroutines.flow.MutableStateFlow

object NfcTransferState {
    // True while NFC data transfer is the active transport for the current presentment.
    // The activity observes this to switch the preferred HCE service between the engagement
    // service (NdefDeviceEngagementService) and the data retrieval service (NfcDataRetrievalService).
    val nfcDataTransferActive = MutableStateFlow(false)
}