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
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.util.flattenEntries

class RetrieveAuthenticationRequestParametersUseCase(
    private val client: HttpClient,
    private val verifierJwsService: VerifierJwsService,
) {
    suspend operator fun invoke(input: String): AuthenticationRequestParameters {
        return retrieveAuthenticationRequestParameters(input)
    }

    // copied from OidcSiopWallet
    private suspend fun retrieveAuthenticationRequestParameters(input: String): AuthenticationRequestParameters {
        val params = kotlin.run {
            // maybe it's already a request jws?
            parseRequestObjectJws(input)
        } ?: kotlin.runCatching {
            // maybe it's a url that already encodes the authentication request as url parameters
            Url(input).parameters.flattenEntries().toMap()
                .decodeFromUrlQuery<AuthenticationRequestParameters>()
        }.getOrNull() ?: kotlin.runCatching {
            // maybe it's a url that yields the request object in some other way
            // currently supported in order of priority:
            // 1. use redirect location as new starting point if available
            // 2. use resonse body as new starting point
            // - maybe it's just a jws that needs to be parsed, but let's also support a url there
            val url = Url(input)
            val response = client.get(url)
            val candidates = listOfNotNull(
                response.headers[HttpHeaders.Location],
                response.bodyAsText(),
            )
            var result: AuthenticationRequestParameters? = null
            for (candidate in candidates) {
                result = kotlin.runCatching {
                    retrieveAuthenticationRequestParameters(candidate)
                }.getOrDefault(result)
                if (result != null) break
            }
            result
        }.getOrNull() ?: throw OAuth2Exception(OpenIdConstants.Errors.INVALID_REQUEST)
            .also { Napier.w("Could not parse authentication request: $input") }

        val requestParams = params.requestUri?.let {
            // go down the rabbit hole following the request_uri parameters
            retrieveAuthenticationRequestParameters(it).also { newParams ->
                if (params.clientId != newParams.clientId) {
                    throw OAuth2Exception(OpenIdConstants.Errors.INVALID_REQUEST)
                        .also { Napier.e("Client ids do not match: before: $params, after: $newParams") }
                }
            }
        } ?: params

        val authenticationRequestParameters = requestParams.let { extractRequestObject(it) ?: it }
        if (authenticationRequestParameters.clientId != requestParams.clientId) {
            throw OAuth2Exception(OpenIdConstants.Errors.INVALID_REQUEST)
                .also { Napier.w("Client ids do not match: outer: $requestParams, inner: $authenticationRequestParameters") }
        }
        return authenticationRequestParameters
    }

    // copied from OidcSiopWallet
    private fun extractRequestObject(params: AuthenticationRequestParameters): AuthenticationRequestParameters? {
        return params.request?.let { requestObject ->
            parseRequestObjectJws(requestObject)
        }
    }

    // copied from OidcSiopWallet
    private fun parseRequestObjectJws(requestObject: String): AuthenticationRequestParameters? {
        JwsSigned.parse(requestObject)?.let { jws ->
            if (verifierJwsService.verifyJwsObject(jws)) {
                return AuthenticationRequestParameters.deserialize(jws.payload.decodeToString())
            }
        }
        return null
    }
}