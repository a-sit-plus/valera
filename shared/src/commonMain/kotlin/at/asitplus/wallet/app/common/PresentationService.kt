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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json

class PresentationService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    private val errorService: ErrorService
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
    suspend fun startSiop(url: String, fromQrCodeScanner: Boolean) {
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
                        if (response.status == HttpStatusCode.InternalServerError) {
                            throw Exception(
                                "InternalServerErrorException",
                                Exception(response.bodyAsText()),
                            )
                        }
                        val location = response.headers[HttpHeaders.Location]
                        if (location != null && !fromQrCodeScanner) {
                            platformAdapter.openUrl(location)
                        }
                    }

                    is OidcSiopWallet.AuthenticationResponseResult.Redirect -> {
                        Napier.d("Opening $authenticationResponse")
                        if (!fromQrCodeScanner) {
                            platformAdapter.openUrl(it.url)
                        }
                    }
                }
            }, onFailure = {
                throw Exception("Failure in received authentication response")
            })
    }
}