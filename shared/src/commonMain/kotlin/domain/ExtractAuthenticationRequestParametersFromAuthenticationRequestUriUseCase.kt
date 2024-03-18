package domain

import at.asitplus.crypto.datatypes.jws.JwsSigned
import at.asitplus.wallet.lib.jws.VerifierJwsService
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.OpenIdConstants
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import composewalletapp.shared.generated.resources.ERROR_QR_CODE_SCANNING_INCONSISTENT_CLIENT_ID
import composewalletapp.shared.generated.resources.Res
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import io.ktor.util.flattenEntries
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalResourceApi::class)
class ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(
    private val verifierJwsService: VerifierJwsService,
) {
    operator fun invoke(requestRedirectUri: String): AuthenticationRequestParameters {
        val requestParams = kotlin.runCatching {
            Url(requestRedirectUri).parameters.flattenEntries().toMap()
                .decodeFromUrlQuery<AuthenticationRequestParameters>()
        }.getOrNull()
            ?: throw OAuth2Exception(OpenIdConstants.Errors.INVALID_REQUEST)
                .also { Napier.w("Could not parse authentication request") }

        val requestLocationClientId = requestParams.clientId

        val authenticationRequestParameters =
            requestParams.let { extractRequestObject(it) ?: it }
        if (authenticationRequestParameters.clientId != requestLocationClientId) {
            throw Exception("${runBlocking { getString(Res.string.ERROR_QR_CODE_SCANNING_INCONSISTENT_CLIENT_ID) }}: UrlParameter: $requestLocationClientId, AuthenticationRequestParameters: ${authenticationRequestParameters.clientId}")
        }
        return authenticationRequestParameters
    }

    // copied from OidcSiopWallet
    private fun extractRequestObject(params: AuthenticationRequestParameters): AuthenticationRequestParameters? {
        params.request?.let { requestObject ->
            JwsSigned.parse(requestObject)?.let { jws ->
                if (verifierJwsService.verifyJwsObject(jws)) {
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