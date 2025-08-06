package ui.viewmodels

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.CapabilitiesService

class CapabilitiesViewModel(
    val capabilitiesService: CapabilitiesService,
) : ViewModel() {

    fun getSignerCheck() = capabilitiesService.getSignerCheck()

    fun isOnlineCheck() = capabilitiesService.isOnlineCheck()

    fun getAttestationCheck() = capabilitiesService.getAttestationCheck()
}