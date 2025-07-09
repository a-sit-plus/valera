package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AddCredentialViewModel(
    val walletMain: WalletMain,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val hostString = settingsRepository.host.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
}