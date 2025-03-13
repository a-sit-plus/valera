package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain

class AddCredentialViewModel(
    val walletMain: WalletMain,
    val onSubmitServer: ((String) -> Unit),
    val navigateUp: () -> Unit,
    val hostString: String,
    val onClickLogo: () -> Unit
)