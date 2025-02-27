package ui.viewmodels.Authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.dif.InputDescriptor
import at.asitplus.wallet.app.common.WalletMain

class AuthenticationConsentViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val transactionData: TransactionData?,
    val requests: List<InputDescriptor>,
    val navigateUp: () -> Unit,
    val buttonConsent: () -> Unit,
    val walletMain: WalletMain,
) {
    val consentToDataTransmission: () -> Unit = {
        buttonConsent()
    }
}