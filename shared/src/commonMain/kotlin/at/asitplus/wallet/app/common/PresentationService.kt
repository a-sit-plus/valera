package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

class PresentationService(val platformAdapter: PlatformAdapter, val dataStoreService: DataStoreService, val cryptoService: CryptoService, val holderAgent: HolderAgent) {
    private val cookieStorage = PersistentCookieStorage(dataStoreService)
    private val client = HttpClient {
        followRedirects = false
        install(ContentNegotiation) {
            json()
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(HttpCookies) {
            storage = cookieStorage
        }
    }

    @Throws(Throwable::class)
    suspend fun startSiop(url: String){
        Napier.d("Start SIOP process")
        val oidcSiopWallet = OidcSiopWallet.newInstance(
            holder = holderAgent,
            cryptoService = cryptoService,
        )
        val authenticationResponse = oidcSiopWallet.createAuthnResponse(url)
        if (authenticationResponse.isFailure) {
            throw Exception("Failure in received authentication response")
        }

        when (val response= authenticationResponse.getOrThrow()){
            is OidcSiopWallet.AuthenticationResponseResult.Post -> {
                Napier.d("Post $authenticationResponse")
                client.post(response.url) {
                    setBody(response.content)
                }
            }
            is OidcSiopWallet.AuthenticationResponseResult.Redirect -> {
                Napier.d("Opening $authenticationResponse")
                platformAdapter.openUrl(response.url)
            }
        }
    }
}