package domain

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier
import kotlinx.serialization.encodeToString
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

        val preparationState = oidcSiopWallet.startAuthorizationResponsePreparation(request).getOrElse {
            return KmmResult.failure(it)
        }

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return KmmResult.success(
            AuthenticationConsentPage(
                authenticationRequestParametersFromSerialized = vckJsonSerializer.encodeToString(request),
                authorizationPreparationStateSerialized = vckJsonSerializer.encodeToString(preparationState),
                recipientName = "SERVICE_NAME_DUMMY_VALUE",
                recipientLocation = request.parameters.clientId ?: "",
            )
        )
    }
}