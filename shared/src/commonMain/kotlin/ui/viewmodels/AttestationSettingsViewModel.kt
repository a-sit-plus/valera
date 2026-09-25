package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import data.storage.DataStoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttestationSettingsViewModel(
    private val attestationService: AttestationService,
    private val settingsRepository: SettingsRepository,
    private val dataStoreService: DataStoreService,
) : ViewModel() {
    val scope = CoroutineScope(Dispatchers.IO)

    val onError = MutableSharedFlow<Throwable>()
    val walletProviderAttestationEnabled = settingsRepository.walletProviderAttestationEnabled.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setWalletProviderAttestationEnabled(enabled: Boolean) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = withContext(Dispatchers.IO + NonCancellable) {
                runCatching {
                    updateWalletProviderAttestation(enabled, settingsRepository, dataStoreService, attestationService::reset)
                }
            }
            result.onFailure { onError.emit(it) }
        }
    }

    fun getWalletProviderHost() = attestationService.getWalletProviderHost()

    fun setWalletProviderHost(host: String) = attestationService.setWalletProviderHost(host)
}

internal suspend fun updateWalletProviderAttestation(
    enabled: Boolean,
    settingsRepository: SettingsRepository,
    dataStoreService: DataStoreService,
    resetAttestation: suspend () -> Unit,
) {
    settingsRepository.set(walletProviderAttestationEnabled = enabled).getOrThrow()
    if (!enabled) {
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_INSTANCE_ATTESTATION_BY_STATE)
        resetAttestation()
    }
}
