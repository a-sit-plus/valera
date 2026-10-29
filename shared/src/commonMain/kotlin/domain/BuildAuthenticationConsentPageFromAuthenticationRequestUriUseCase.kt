package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.presentation.ValidatedPresentationRequest
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.validation.integrityRejection
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
) {
    /**
     * Loads and validates the request [requestUri]. A request that is rejected for its relying party, e.g. because
     * it is not trusted, is shown to the user with a warning, while one that fails a check of its integrity, e.g. its
     * signature, fails. The validated request is kept in [PresentationService.validatedRequests] for the route.
     */
    suspend operator fun invoke(
        requestUri: String,
    ): KmmResult<AuthenticationViewRoute> = catching {
        val validation = presentationService.startAuthorizationResponsePreparation(requestUri)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()
        val preparationState = validation.preparationState
        val relyingParty = RelyingPartyValidationUi.of(validation.validationReport, validation.relyingParty)
        if (relyingParty.outcome?.integrityFailed == true) {
            throw validation.validationReport.integrityRejection()
                ?: IllegalArgumentException("The request has been rejected")
        }

        AuthenticationViewRoute(
            requestId = presentationService.validatedRequests.put(
                ValidatedPresentationRequest(preparationState, relyingParty)
            ),
            recipientLocation = preparationState.request.parameters.clientId ?: "",
            isCrossDeviceFlow = false,
        )
    }
}
