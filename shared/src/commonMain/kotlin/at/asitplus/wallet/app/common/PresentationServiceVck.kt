package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.AuthenticationResponseResult
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class PresentationServiceVck(
    /**
     * Used to display the success page to the user
     */
    private val openUrlExternally: suspend (String) -> Unit,
    val client: HttpClient,
    cryptoService: CryptoService,
    holderAgent: HolderAgent,
) {
    private val oidcSiopWallet = OidcSiopWallet(
        holder = holderAgent,
        agentPublicKey = cryptoService.keyMaterial.publicKey,
        jwsService = DefaultJwsService(cryptoService),
        remoteResourceRetriever = { url ->
            withContext(Dispatchers.IO) {
                client.get(url).bodyAsText()
            }
        },
        requestObjectJwsVerifier = { _ -> true }, // unsure about this one?
        scopePresentationDefinitionRetriever = { null }
    )

    @Throws(Throwable::class)
    suspend fun startPresentation(request: AuthenticationRequestParametersFrom) {
        Napier.i("startSiop: $request")
        oidcSiopWallet.createAuthnResponse(request).getOrThrow().let {
            when (it) {
                is AuthenticationResponseResult.Post -> postResponse(it)
                is AuthenticationResponseResult.Redirect -> redirectResponse(it)
            }
        }
    }

    private suspend fun postResponse(it: AuthenticationResponseResult.Post) {
        Napier.i("postResponse: $it")
        handlePostResponse(client.submitForm(
            url = it.url,
            formParameters = parameters {
                it.params.forEach { append(it.key, it.value) }
            }
        ))
    }

    private suspend fun handlePostResponse(response: HttpResponse) {
        Napier.i("handlePostResponse: response $response")
        when (response.status.value) {
            HttpStatusCode.InternalServerError.value ->
                throw Exception("InternalServerErrorException", Exception(response.bodyAsText()))

            in 200..399 -> response.headers[HttpHeaders.Location]?.let {
                if (it.isNotEmpty()) {
                    openUrlExternally.invoke(it)
                }
            } ?: runCatching { response.body<OpenId4VpSuccess>() }.getOrNull()?.let {
                if (it.redirectUri.isNotEmpty()) {
                    openUrlExternally.invoke(it.redirectUri)
                }
            }

            else -> throw Exception(response.readBytes().decodeToString())
        }
    }

    @Serializable
    data class OpenId4VpSuccess(
        @SerialName("redirect_uri")
        val redirectUri: String,
    )

    private suspend fun redirectResponse(it: AuthenticationResponseResult.Redirect) {
        Napier.i("redirectResponse: ${it.url}")
        openUrlExternally.invoke(it.url)
    }

}