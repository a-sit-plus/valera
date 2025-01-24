package ui.viewmodels

import at.asitplus.wallet.app.common.QtspConfig
import at.asitplus.wallet.app.common.WalletMain

class SigningQtspSelectionViewModel(
    val navigateUp: () -> Unit,
    val onContinue: (QtspConfig) -> Unit,
    val walletMain: WalletMain,
    val qtspConfig: QtspConfig
)