package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableSharedFlow

class CapabilitiesService(
    val keyStoreService: KeystoreService,
) {
    val signerStatus = MutableSharedFlow<Boolean>(replay = 1)
    val onlineStatus = MutableSharedFlow<Boolean>(replay = 1)
    val attestationStatus = MutableSharedFlow<Boolean>(replay = 1)

    suspend fun refreshStatus() {
        signerStatus.emit(getSignerStatus())
        onlineStatus.emit(getOnlineStatus())
        attestationStatus.emit(getAttestationStatus())
    }

    suspend fun getSignerStatus() = keyStoreService.testSigner()

    suspend fun getOnlineStatus() =
        catchingUnwrapped {
            val httpClient = HttpClient()
            val host = "https://wallet.a-sit.at/"
            val url = "${host}check.json"
            httpClient.get(url)
        }.isSuccess


    suspend fun getAttestationStatus() = keyStoreService.testAttestation()
}