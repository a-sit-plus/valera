package ui.viewmodels.iso

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class TransferViewModel(
    val walletMain: WalletMain,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settingsReady = MutableStateFlow(false)
    val settingsReady: StateFlow<Boolean> = _settingsReady.asStateFlow()

    fun initSettings() {
        if (_settingsReady.value) return
        walletMain.scope.launch {
            settingsRepository.awaitPresentmentSettingsFirst()
            _settingsReady.value = true
        }
    }
}
