package ui.screens

import at.asitplus.wallet.app.common.WalletMain

class LogViewModel(
    val navigateUp: () -> Unit,
    val walletMain: WalletMain,
) {
    var logArray: List<String> = walletMain.getLog()
    val shareLog = { walletMain.platformAdapter.shareLog() }
}