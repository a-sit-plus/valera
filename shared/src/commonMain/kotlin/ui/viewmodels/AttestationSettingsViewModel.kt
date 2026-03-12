package ui.viewmodels

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.attestation.AttestationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class AttestationSettingsViewModel(
    val attestationService: AttestationService
) : ViewModel() {
    val scope = CoroutineScope(Dispatchers.IO)

    fun preload() = scope.launch {
        attestationService.preloadAttestation()
    }
}