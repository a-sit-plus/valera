package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.validation.rejection
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
) {
    /**
     * Loads and validates the request [requestUri]. A request that is rejected for its relying party, e.g. because
     * it is not trusted, is shown to the user with a warning, while one that fails a check of its integrity, e.g. its
     * signature, fails.
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
            throw validation.validationReport.rejection()
                ?: IllegalArgumentException("The request has been rejected")
        }

        AuthenticationViewRoute(
            authenticationRequest = preparationState.request,
            authorizationResponsePreparationState = preparationState,
            recipientLocation = preparationState.request.parameters.clientId ?: "",
            isCrossDeviceFlow = false,
            relyingPartyValidation = relyingParty,
        )
    }
}
