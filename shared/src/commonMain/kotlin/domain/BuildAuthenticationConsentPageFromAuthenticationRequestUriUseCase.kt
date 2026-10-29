package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.presentation.ValidatedPresentationRequest
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.validation.integrityRejection
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
    /** The session scope, which resolves the status of the relying party while the consent screen is shown. */
    val sessionScope: CoroutineScope,
) {
    /**
     * Loads and validates the request [requestUri]. A request that is rejected for its relying party, e.g. because
     * it is not trusted, is shown to the user with a warning, while one that fails a check of its integrity, e.g. its
     * signature, fails. The validated request is kept in [PresentationService.validatedRequests] for the route, which
     * the status of the registration certificates of its relying party is resolved for in [sessionScope].
     */
    suspend operator fun invoke(
        requestUri: String,
    ): KmmResult<AuthenticationViewRoute> = catching {
        val pending = presentationService.startAuthorizationResponsePreparation(requestUri)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()
        val validation = pending.validation
        val preparationState = validation.preparationState
        // A pending status never fails the integrity of a request, so this is decided before it is resolved
        val relyingParty = RelyingPartyValidationUi.of(validation.validationReport, validation.relyingParty)
        if (relyingParty.outcome?.integrityFailed == true) {
            throw validation.validationReport.integrityRejection()
                ?: IllegalArgumentException("The request has been rejected")
        }

        AuthenticationViewRoute(
            requestId = presentationService.validatedRequests.put(
                ValidatedPresentationRequest.resolvingStatus(pending, sessionScope)
            ),
            recipientLocation = preparationState.request.parameters.clientId ?: "",
            isCrossDeviceFlow = false,
        )
    }
}
