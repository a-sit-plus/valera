package ui.viewmodels

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.WalletMain

class CapabilitiesViewModel(
    val capabilitiesService: CapabilitiesService,
    val walletMain: WalletMain
) : ViewModel() {
    var needReset: Boolean? = null
}