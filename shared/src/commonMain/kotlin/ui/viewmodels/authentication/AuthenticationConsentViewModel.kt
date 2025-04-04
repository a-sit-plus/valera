package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.CredentialPresentationRequest

class AuthenticationConsentViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val transactionData: at.asitplus.openid.TransactionData?,
    val navigateUp: () -> Unit,
    val buttonConsent: () -> Unit,
    val walletMain: WalletMain,
    val presentationRequest: CredentialPresentationRequest,
    val onClickLogo: () -> Unit,
) {
    val consentToDataTransmission: () -> Unit = {
        buttonConsent()
    }
}