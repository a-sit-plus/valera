package domain

import at.asitplus.wallet.lib.data.dif.PresentationDefinition
import navigation.AuthenticationConsentPage

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    private val extractAuthenticationRequestParametersFromAuthenticationRequestUri: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    private val retrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase: RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase,
) {
    suspend operator fun invoke(requestUri: String): AuthenticationConsentPage {
        val finalRequestUri = retrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase(requestUri)
        val authenticationRequestParameters = extractAuthenticationRequestParametersFromAuthenticationRequestUri(finalRequestUri)

        val requestedClaims = authenticationRequestParameters.presentationDefinition?.claims ?: listOf()

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return AuthenticationConsentPage(
            url = finalRequestUri,
            claims = requestedClaims,
            recipientName = "DemoService",
            recipientLocation = authenticationRequestParameters.clientId,
        )
    }
}

private val PresentationDefinition.claims: List<String>
    get() = this.inputDescriptors
        .mapNotNull { it.constraints }.flatMap { it.fields?.toList() ?: listOf() }
        .flatMap { it.path.toList() }
        .filter { it != "$.type" }
        .filter { it != "$.mdoc.doctype" }
        .map { it.removePrefix("\$.mdoc.") }
        .map { it.removePrefix("\$.") }