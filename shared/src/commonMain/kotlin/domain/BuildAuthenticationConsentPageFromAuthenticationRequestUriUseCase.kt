package domain

import at.asitplus.wallet.lib.data.jsonSerializer
import io.github.aakira.napier.Napier
import kotlinx.serialization.encodeToString
import ui.navigation.AuthenticationConsentPage

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    private val retrieveAuthenticationRequestParametersUseCase: RetrieveAuthenticationRequestParametersUseCase,
) {
    suspend operator fun invoke(requestUri: String): AuthenticationConsentPage {
        val authenticationRequestParameters =  retrieveAuthenticationRequestParametersUseCase(requestUri).also {
            Napier.d("authenticationRequestParameters: $it")
        }

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return AuthenticationConsentPage(
            authenticationRequestParametersSerialized = jsonSerializer.encodeToString(authenticationRequestParameters),
            recipientName = "SERVICE_NAME_DUMMY_VALUE",
            recipientLocation = authenticationRequestParameters.clientId ?: "",
        )
    }
}