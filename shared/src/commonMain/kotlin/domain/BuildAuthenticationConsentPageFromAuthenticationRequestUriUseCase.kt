package domain

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier
import kotlinx.serialization.encodeToString
import ui.navigation.Routes.AuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val oidcSiopWallet: OidcSiopWallet,
) {
    suspend operator fun invoke(requestUri: String): KmmResult<AuthenticationConsentRoute> {
        val request =
            oidcSiopWallet.parseAuthenticationRequestParameters(requestUri).getOrElse {
                Napier.d("authenticationRequestParameters: $it")
                return KmmResult.failure(it)
            }

        val preparationState = oidcSiopWallet.startAuthorizationResponsePreparation(request).getOrElse {
            return KmmResult.failure(it)
        }

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return KmmResult.success(
            AuthenticationConsentRoute(
                authenticationRequestParametersFromSerialized = vckJsonSerializer.encodeToString(request),
                authorizationPreparationStateSerialized = vckJsonSerializer.encodeToString(preparationState),
                recipientLocation = request.parameters.clientId ?: "",
            )
        )
    }
}