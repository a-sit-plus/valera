package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.Configuration.DATASTORE_CAPABILITIES_ATTESTATION
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.DataStoreService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CapabilitiesService(
    private val keyStoreService: KeystoreService,
    private val dataStoreService: DataStoreService,
) {
    val signerStatus = MutableSharedFlow<Boolean>(replay = 1)
    val onlineStatus = MutableSharedFlow<Boolean>(replay = 1)
    val attestationStatus = MutableSharedFlow<Boolean>(replay = 1)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            refreshStatus()
        }
    }

    suspend fun refreshStatus() {
        signerStatus.emit(getSignerStatus())
        onlineStatus.emit(getOnlineStatus())
        attestationStatus.emit(getAttestationStatus())
    }

    private suspend fun getSignerStatus() = keyStoreService.testSigner()

    private suspend fun getOnlineStatus() = catchingUnwrapped {
        HttpClient().get("https://wallet.a-sit.at/check.json")
    }.isSuccess


    private suspend fun getAttestationStatus() = getAttestationPreference().onSuccess { capability ->
            capability
        }.onFailure {
            val capability = keyStoreService.testAttestation()
            setAttestationPreference(capability)
            capability
        }.getOrElse { true }


    private suspend fun getAttestationPreference() = runCatching {
        vckJsonSerializer.decodeFromString<Boolean>(
            dataStoreService.getPreference(DATASTORE_CAPABILITIES_ATTESTATION).first()!!
        )
    }

    private suspend fun setAttestationPreference(value: Boolean) {
        dataStoreService.setPreference(
            key = DATASTORE_CAPABILITIES_ATTESTATION, value = vckJsonSerializer.encodeToString(value)
        )
    }

    suspend fun reset() {
        dataStoreService.deletePreference(DATASTORE_CAPABILITIES_ATTESTATION)
    }
}