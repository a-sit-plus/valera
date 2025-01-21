package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain

class SigningQtspSelectionViewModel(
    val navigateUp: () -> Unit,
    val onContinue: (String) -> Unit,
    val walletMain: WalletMain,
    val hostString: String
)