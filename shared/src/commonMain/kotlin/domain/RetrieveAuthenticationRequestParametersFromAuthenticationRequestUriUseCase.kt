package domain

import at.asitplus.crypto.datatypes.jws.JwsSigned
import at.asitplus.wallet.lib.jws.VerifierJwsService
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.OpenIdConstants
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.util.flattenEntries

class RetrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(
    private val client: HttpClient,
    private val verifierJwsService: VerifierJwsService,
) {
    suspend operator fun invoke(authenticationRequestUri: String): AuthenticationRequestParameters {
        val requestParams = kotlin.runCatching {
            val params = Url(authenticationRequestUri).parameters.flattenEntries().toMap()
                .decodeFromUrlQuery<AuthenticationRequestParameters>()

            Napier.d("params: $params")
            params.requestUri?.let {
                this.invoke(it).also { newParams ->
                    if (params.clientId != newParams.clientId) {
                        throw Exception("Client ids are inconsistent: before: $params, after: $newParams")
                    }
                }
            } ?: params
        }.getOrElse {
            val urlParts = authenticationRequestUri.split("/")
            Napier.d("urlParts: $urlParts")
            if (urlParts.isNotEmpty() && urlParts[urlParts.lastIndex - 1] == "request.jwt") {
                // https://domain/path/request.jwt/somethingelse
                // it seems like the request params are delivered through the response body payload in this case
                val response = client.get(authenticationRequestUri)
                val requestJwt = response.bodyAsText()
                Napier.d("requestJwt: $requestJwt")
                extractJwtPayload(requestJwt) ?: throw Exception("response: $response")
            } else {
                null
            }
        } ?: throw OAuth2Exception(OpenIdConstants.Errors.INVALID_REQUEST)
            .also { Napier.w("Could not parse authentication request: $authenticationRequestUri") }

        val requestLocationClientId = requestParams.clientId

        val authenticationRequestParameters =
            requestParams.let { extractRequestObject(it) ?: it }
        if (authenticationRequestParameters.clientId != requestLocationClientId) {
            throw Exception("Client id does not match: UrlParameter: $requestLocationClientId, AuthenticationRequestParameters: ${authenticationRequestParameters.clientId}")
        }
        return authenticationRequestParameters
    }

    // copied from OidcSiopWallet
    private fun extractRequestObject(params: AuthenticationRequestParameters): AuthenticationRequestParameters? {
        return params.request?.let { requestObject ->
            extractJwtPayload(requestObject)
        }
    }

    // copied from OidcSiopWallet
    private fun extractJwtPayload(jwtSerialized: String): AuthenticationRequestParameters? {
        return JwsSigned.parse(jwtSerialized)?.let { jws ->
            if (verifierJwsService.verifyJwsObject(jws)) {
                kotlin.runCatching {
                    at.asitplus.wallet.lib.oidc.jsonSerializer.decodeFromString<AuthenticationRequestParameters>(
                        jws.payload.decodeToString()
                    )
                }.getOrNull()
            } else {
                null
            }
        }
    }
}