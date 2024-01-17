package at.asitplus.wallet.app.common

import ErrorService
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import data.storage.DataStoreService
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

class PresentationService(
    val platformAdapter: PlatformAdapter,
    val dataStoreService: DataStoreService,
    val cryptoService: CryptoService,
    val holderAgent: HolderAgent,
    val errorService: ErrorService
) {
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
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
    suspend fun startSiop(url: String) {
        Napier.d("Start SIOP process")
        val oidcSiopWallet = OidcSiopWallet.newInstance(
            holder = holderAgent,
            cryptoService = cryptoService,
        )
        val authenticationResponse = oidcSiopWallet.createAuthnResponse(url)

        authenticationResponse.fold(
            onSuccess = {
                when (it) {
                    is OidcSiopWallet.AuthenticationResponseResult.Post -> {
                        Napier.d("Post $authenticationResponse")
                        val response = client.post(it.url) {
                            setBody(it.content)
                        }
                        val location = response.headers["Location"]
                        if (location != null) {
                            platformAdapter.openUrl(location)
                        } else {
                            throw Exception("Location is NULL")
                        }
                    }

                    is OidcSiopWallet.AuthenticationResponseResult.Redirect -> {
                        Napier.d("Opening $authenticationResponse")
                        platformAdapter.openUrl(it.url)
                    }
                }
            }, onFailure = {
                throw Exception("Failure in received authentication response")
            })
    }
}