package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttestationSettingsViewModel(
    val attestationService: AttestationService,
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

    fun preloadKeyAttestation() = scope.launch {
        attestationService.preloadKeyAttestation().onFailure {
            onError.emit(Throwable("Unable to obtain key attestation from wallet provider.", it))
        }
    }
    fun preloadInstanceAttestation() = scope.launch {
        attestationService.preloadInstanceAttestation().onFailure {
            onError.emit(Throwable("Unable to obtain instance attestation from wallet provider.", it))
        }
    }
}
