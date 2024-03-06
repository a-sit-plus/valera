package domain

import Resources
import at.asitplus.wallet.lib.jws.DefaultVerifierJwsService
import at.asitplus.wallet.lib.jws.JwsSigned
import at.asitplus.wallet.lib.jws.VerifierJwsService
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.OpenIdConstants
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import io.ktor.util.flattenEntries

class ExtractRequestObjectFromRedirectUriUseCase(
    private val verifierJwsService: VerifierJwsService = DefaultVerifierJwsService(),
) {
    operator fun invoke(requestRedirectUri: String): AuthenticationRequestParameters {
        val requestParams = kotlin.runCatching {
            Url(requestRedirectUri).parameters.flattenEntries().toMap()
                .decodeFromUrlQuery<AuthenticationRequestParameters>()
        }.getOrNull()
            ?: throw OAuth2Exception(OpenIdConstants.Errors.INVALID_REQUEST)
                .also { Napier.w("Could not parse authentication request") }

        val requestLocationClientId = requestParams.clientId

        val requestLocationRequestParameterParsed =
            requestParams.let { extractRequestObject(it) ?: it }
        if (requestLocationRequestParameterParsed.clientId != requestLocationClientId) {
            throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INCONSISTENT_CLIENT_ID}: UrlParameter: $requestLocationClientId, AuthenticationRequestParameters: ${requestLocationRequestParameterParsed.clientId}")
        }
        return requestLocationRequestParameterParsed
    }

    // copied from OidcSiopWallet
    private fun extractRequestObject(params: AuthenticationRequestParameters): AuthenticationRequestParameters? {
        params.request?.let { requestObject ->
            JwsSigned.parse(requestObject)?.let { jws ->
                if (verifierJwsService.verifyJwsObject(jws, requestObject)) {
                    return kotlin.runCatching {
                        at.asitplus.wallet.lib.oidc.jsonSerializer.decodeFromString<AuthenticationRequestParameters>(
                            jws.payload.decodeToString()
                        )
                    }.getOrNull()
                }
            }
        }
        return null
    }
}