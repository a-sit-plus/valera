package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

class CapabilitiesService(
    val keyStoreService: KeystoreService,
) {
    fun getSignerCheck(): Boolean = runBlocking { keyStoreService.testSigner() }

    fun isOnlineCheck() = runBlocking {
        catchingUnwrapped {
        val httpClient = HttpClient()
        val host = "https://wallet.a-sit.at/"
        val url = "${host}check.json"
        httpClient.get(url)
        }.isSuccess
    }

    fun getAttestationCheck(): Boolean = runBlocking { keyStoreService.testAttestation()}
}