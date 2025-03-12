package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain

class SigningQtspSelectionViewModel(
    val navigateUp: () -> Unit,
    val onContinue: () -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit
)