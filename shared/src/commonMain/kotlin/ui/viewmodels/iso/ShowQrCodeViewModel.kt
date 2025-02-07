package ui.viewmodels.iso

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.WalletMain
import data.bletransfer.Holder

class ShowQrCodeViewModel(
    val walletMain: WalletMain,
    val holder: Holder,
    val navigateUp: () -> Unit,
    val onConnection: (Holder) -> Unit
) {
    var permission by mutableStateOf(false)
    var qrcodeText by mutableStateOf("")
    var shouldDisconnect by mutableStateOf(true)
}
