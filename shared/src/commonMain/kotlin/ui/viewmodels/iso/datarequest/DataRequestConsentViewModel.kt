package ui.viewmodels.iso.datarequest

import at.asitplus.wallet.app.common.WalletMain
import data.bletransfer.util.RequestedDocument

class DataRequestConsentViewModel(
    val requestedAttributes: List<RequestedDocument>,
    val navigateUp: () -> Unit,
    buttonConsent: () -> Unit,
    val walletMain: WalletMain
) {
    val consentToDataTransmission: () -> Unit = {
        buttonConsent()
    }
}
