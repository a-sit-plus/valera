package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.PresentationService
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequest(
    val presentationService: PresentationService,
) {
    suspend operator fun invoke(
        request: RequestParametersFrom<AuthenticationRequestParameters>,
    ): KmmResult<AuthenticationViewRoute> = catching {
        val preparationState = presentationService.startAuthorizationResponsePreparation(request)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()

        AuthenticationViewRoute(
            authenticationRequest = preparationState.request,
            authorizationResponsePreparationState = preparationState,
            recipientLocation = preparationState.request.parameters.clientId ?: "",
            isCrossDeviceFlow = false,
        )
    }
}
