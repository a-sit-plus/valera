package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain

class SigningQtspSelectionViewModel(
    val navigateUp: () -> Unit,
    val onContinue: (String) -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit,
    val url: String
)