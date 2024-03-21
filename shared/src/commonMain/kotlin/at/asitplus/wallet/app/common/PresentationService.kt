package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.JsonWebKeySet
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import at.asitplus.wallet.lib.oidc.jsonSerializer
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PresentationService(
    val platformAdapter: PlatformAdapter,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val client = httpService.buildHttpClient()

    @Throws(Throwable::class)
    suspend fun startSiop(
        authenticationRequestParameters: AuthenticationRequestParameters,
        fromQrCodeScanner: Boolean
    ) {
        Napier.d("Start SIOP process: $authenticationRequestParameters")
        val oidcSiopWallet = OidcSiopWallet.newInstance(
            holder = holderAgent,
            cryptoService = cryptoService,
            jwkSetRetriever = { jwksUrl ->
                runBlocking {
                    withContext(Dispatchers.IO) {
                        val response = client.get(jwksUrl)
                        jsonSerializer.decodeFromString<JsonWebKeySet>(response.bodyAsText())
                    }
                }
            }
        )
        oidcSiopWallet.createAuthnResponse(authenticationRequestParameters).fold(
            onSuccess = {
                when (it) {
                    is OidcSiopWallet.AuthenticationResponseResult.Post -> {
                        Napier.d("Post ${it.url}")
                        val response = client.post(it.url) {
                            setBody(it.content)
                        }
                        Napier.d("response $response")
                        when (response.status.value) {
                            HttpStatusCode.InternalServerError.value -> {
                                throw Exception(
                                    "InternalServerErrorException",
                                    Exception(response.bodyAsText()),
                                )
                            }

                            in 200..399 -> {
                                val location = response.headers[HttpHeaders.Location]
                                if (location != null && fromQrCodeScanner == false) {
                                    platformAdapter.openUrl(location)
                                }
                            }

                            else -> {
                                throw Exception(response.readBytes().decodeToString())
                            }
                        }
                    }

                    is OidcSiopWallet.AuthenticationResponseResult.Redirect -> {
                        Napier.d("Opening ${it.url}")
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