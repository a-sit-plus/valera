package domain

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import data.vclib.AuthenticationRequest
import data.vclib.AuthenticationResponseResultSerializable
import io.github.aakira.napier.Napier
import ui.navigation.AuthenticationConsentPage

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val oidcSiopWallet: OidcSiopWallet,
) {
    suspend operator fun invoke(requestUri: String): KmmResult<AuthenticationConsentPage> {
        val request =
            oidcSiopWallet.parseAuthenticationRequestParameters(requestUri).getOrElse {
                Napier.d("authenticationRequestParameters: $it")
                return KmmResult.failure(it)
            }

        val response = oidcSiopWallet.createAuthnResponse(request).getOrElse {
            return KmmResult.failure(it)
        }

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return KmmResult.success(
            AuthenticationConsentPage(
                authenticationRequestSerialized = request.let {
                    AuthenticationRequest.createInstance(it)
                }.serialize(),

                authenticationResponseSerialized = response.let {
                    AuthenticationResponseResultSerializable.createInstance(it).serialize()
                },

                recipientName = "SERVICE_NAME_DUMMY_VALUE",
                recipientLocation = request.parameters.clientId ?: "",
            )
        )
    }
}