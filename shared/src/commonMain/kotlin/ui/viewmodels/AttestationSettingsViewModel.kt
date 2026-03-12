package ui.viewmodels

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.attestation.AttestationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class AttestationSettingsViewModel(
    val attestationService: AttestationService
) : ViewModel() {
    val scope = CoroutineScope(Dispatchers.IO)

    val onError = MutableSharedFlow<Throwable>()
    fun preload() = scope.launch {
        attestationService.preloadAttestation().onFailure {
            onError.emit(Throwable("Unable to obtain attestation from wallet provider.", it))
        }
    }
}