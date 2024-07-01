package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.AuthenticationResponseResult
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class PresentationService(
    val platformAdapter: PlatformAdapter,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val client = httpService.buildHttpClient()
    val oidcSiopWallet = OidcSiopWallet.newDefaultInstance(
        holder = holderAgent,
        cryptoService = cryptoService,
        remoteResourceRetriever = { url ->
            withContext(Dispatchers.IO) {
                client.get(url).bodyAsText()
            }
        }
    )

    @Throws(Throwable::class)
    suspend fun startSiop(
        authenticationRequestParameters: AuthenticationRequestParametersFrom<*>,
        fromQrCodeScanner: Boolean
    ) {
        Napier.d("Start SIOP process: $authenticationRequestParameters")
        oidcSiopWallet.createAuthnResponse(authenticationRequestParameters).getOrThrow().let {
            when (it) {
                is AuthenticationResponseResult.Post -> {
                    Napier.d("Post ${it.url}: $it")
                    val response = client.submitForm(
                        url = it.url,
                        formParameters = parameters {
                            it.params.forEach { append(it.key, it.value) }
                        }
                    )
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

                is AuthenticationResponseResult.Redirect -> {
                    Napier.d("Opening ${it.url}")
                    if (!fromQrCodeScanner) {
                        platformAdapter.openUrl(it.url)
                    }
                }
            }
        }
    }
}