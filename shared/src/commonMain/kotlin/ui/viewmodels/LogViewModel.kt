package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain

class LogViewModel(
    val navigateUp: () -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit
) {
    var logArray: List<String> = walletMain.getLog()
    val shareLog = { walletMain.platformAdapter.shareLog() }
}