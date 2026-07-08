package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.relyingParty.validation.WrpValidator
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequest(
    val presentationService: PresentationService,
    val wrpValidator: WrpValidator
) {
    suspend operator fun invoke(
        request: RequestParametersFrom<AuthenticationRequestParameters>,
    ): KmmResult<AuthenticationViewRoute> = catching {
        val preparationState = presentationService.startAuthorizationResponsePreparation(request)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()
        val wrpRequestValidationResult = wrpValidator.validate(preparationState.request).getOrNull()

        AuthenticationViewRoute(
            authenticationRequest = preparationState.request,
            authorizationResponsePreparationState = preparationState,
            recipientLocation = preparationState.request.parameters.clientId ?: "",
            isCrossDeviceFlow = false,
            wrpValidationResult = wrpRequestValidationResult
        )
    }
}
