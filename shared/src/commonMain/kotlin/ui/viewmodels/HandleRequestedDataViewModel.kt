package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain
import data.bletransfer.Holder

class HandleRequestedDataViewModel(
    val holder: Holder,
    val navigateUp: () -> Unit,
    val walletMain: WalletMain
) {
    val requestedAttributes = holder.getAttributes()
}
