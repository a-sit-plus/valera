package domain

import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier

class RetrieveAuthenticationRequestParametersUseCase(
    private val oidcSiopWallet: OidcSiopWallet,
) {
    suspend operator fun invoke(input: String): AuthenticationRequestParameters {
        return retrieveAuthenticationRequestParameters(input)
    }

    private suspend fun retrieveAuthenticationRequestParameters(input: String): AuthenticationRequestParameters {
        return oidcSiopWallet.parseAuthenticationRequestParameters(input).getOrElse {
            Napier.w("Could not parse authentication request parameters from $input", it)
            throw it
        }
    }

}
