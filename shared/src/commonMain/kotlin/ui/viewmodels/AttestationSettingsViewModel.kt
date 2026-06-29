package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AttestationSettingsViewModel(
    private val attestationService: AttestationService,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val scope = CoroutineScope(Dispatchers.IO)

    val onError = MutableSharedFlow<Throwable>()
    val walletProviderAttestationEnabled = settingsRepository.walletProviderAttestationEnabled.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setWalletProviderAttestationEnabled(enabled: Boolean) {
        settingsRepository.set(walletProviderAttestationEnabled = enabled)
    }

    fun getWalletProviderHost() = attestationService.getWalletProviderHost()

    fun setWalletProviderHost(host: String) = attestationService.setWalletProviderHost(host)
}
