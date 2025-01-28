package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain
import data.bletransfer.Holder
import data.bletransfer.getHolder

class ShowQrCodeViewModel(
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val onConnection: (Holder) -> Unit
) {
    val holder: Holder = getHolder()
    var permission: Boolean = false
    var qrcodeText: String = ""
    var shouldDisconnect: Boolean = true
}
