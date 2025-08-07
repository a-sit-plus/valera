package ui.viewmodels

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.WalletMain

class InitializationViewModel(
    val walletMain: WalletMain,
    val capabilitiesService: CapabilitiesService
) : ViewModel() {

    fun getInitializationState(
        isConditionsAccepted: Boolean,
        signerStatus: Boolean,
        attestationStatus: Boolean,
        onlineStatus: Boolean,
    ) = when (isConditionsAccepted) {
        false -> {
            InitializationState.ONBOARDING
        }

        true -> {
            if (attestationStatus && signerStatus && onlineStatus) {
                InitializationState.HOMESCREEN
            } else {
                InitializationState.CAPABILITIES
            }
        }
    }
}


enum class InitializationState {
    ONBOARDING,
    HOMESCREEN,
    CAPABILITIES,
}