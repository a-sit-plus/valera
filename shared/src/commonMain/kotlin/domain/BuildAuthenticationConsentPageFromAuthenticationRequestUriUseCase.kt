package domain

import navigation.AuthenticationConsentPage

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    private val extractAuthenticationRequestParametersFromAuthenticationRequestUri: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    private val extractClaimsFromPresentationDefinitionUseCase: ExtractClaimsFromPresentationDefinitionUseCase,
    private val retrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase: RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase,
) {
    suspend operator fun invoke(requestUri: String): AuthenticationConsentPage {
        val finalRequestUri = retrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase(requestUri)
        val authenticationRequestParameters = extractAuthenticationRequestParametersFromAuthenticationRequestUri(finalRequestUri)

        val requestedClaims = authenticationRequestParameters.presentationDefinition?.let {
            extractClaimsFromPresentationDefinitionUseCase(it)
        } ?: listOf()

        // TODO("extract recipient name from the metadataResponse; the data is not yet being delivered though")
        return AuthenticationConsentPage(
            url = finalRequestUri,
            claims = requestedClaims,
            recipientName = "DemoService",
            recipientLocation = authenticationRequestParameters.clientId,
        )
    }
}