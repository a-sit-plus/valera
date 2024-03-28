package domain

import at.asitplus.wallet.lib.data.jsonSerializer
import kotlinx.serialization.encodeToString
import ui.navigation.AuthenticationConsentPage

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    private val retrieveAuthenticationRequestParametersUseCase: RetrieveAuthenticationRequestParametersUseCase,
) {
    suspend operator fun invoke(requestUri: String): AuthenticationConsentPage {
        val authenticationRequestParameters =  retrieveAuthenticationRequestParametersUseCase(requestUri)

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return AuthenticationConsentPage(
            authenticationRequestParametersSerialized = jsonSerializer.encodeToString(authenticationRequestParameters),
            recipientName = "DemoService",
            recipientLocation = authenticationRequestParameters.clientId,
        )
    }
}