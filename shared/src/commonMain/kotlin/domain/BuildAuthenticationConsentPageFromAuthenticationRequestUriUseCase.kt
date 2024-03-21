package domain

import ui.navigation.AuthenticationConsentPage

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    private val retrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase: RetrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
) {
    suspend operator fun invoke(requestUri: String): AuthenticationConsentPage {
        val authenticationRequestParameters =  retrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(requestUri)

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return AuthenticationConsentPage(
            authenticationRequestParameters = authenticationRequestParameters,
            recipientName = "DemoService",
            recipientLocation = authenticationRequestParameters.clientId,
        )
    }
}