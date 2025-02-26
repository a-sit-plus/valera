package ui.viewmodels.Authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.wallet.app.common.WalletMain
import data.RequestOptionParameters

class AuthenticationConsentViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val requests: Map<String, RequestOptionParameters>,
    val transactionData: TransactionData?,
    val navigateUp: () -> Unit,
    val buttonConsent: () -> Unit,
    val walletMain: WalletMain,
) {
    val consentToDataTransmission: () -> Unit = {
        buttonConsent()
    }
}
