package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain

class SigningIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: () -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    suspend fun process() {
        onSuccess()
    }
}