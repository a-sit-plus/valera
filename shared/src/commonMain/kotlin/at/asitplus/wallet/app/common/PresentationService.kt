package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class PresentationService(
    val platformAdapter: PlatformAdapter,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val client = httpService.buildHttpClient()

    @Throws(Throwable::class)
    suspend fun startSiop(url: String, fromQrCodeScanner: Boolean) {
        Napier.d("Start SIOP process")
        val oidcSiopWallet = OidcSiopWallet.newInstance(
            holder = holderAgent,
            cryptoService = cryptoService,
        )
        oidcSiopWallet.createAuthnResponse(url).fold(
            onSuccess = {
                when (it) {
                    is OidcSiopWallet.AuthenticationResponseResult.Post -> {
                        Napier.d("Post ${it.url}")
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