package at.asitplus.wallet.app.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
enum class LoadingMessageKey {
    AppInitialization,
    IncomingRequest,
    CredentialOffer,
    IssuerMetadata,
    IssuingCredential,
    StoringCredential,
    CheckingRequestedCredentials,
    CheckingDeviceRequirements,
    RequestingCameraPermission,
    ScanningQrContent,
    AttestationSettings,
    Generic,
}

class LoadingStatusService {
    private val mutableMessage = MutableStateFlow<LoadingMessageKey?>(null)
    val message: StateFlow<LoadingMessageKey?> = mutableMessage

    fun set(message: LoadingMessageKey) {
        mutableMessage.value = message
    }

    fun clear() {
        mutableMessage.value = null
    }
}
