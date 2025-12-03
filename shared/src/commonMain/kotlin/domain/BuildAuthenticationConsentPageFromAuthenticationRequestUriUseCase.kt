package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.PresentationService
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
) {
    suspend operator fun invoke(
        requestUri: String,
    ): KmmResult<AuthenticationViewRoute> = catching {
        val preparationState = presentationService.startAuthorizationResponsePreparation(requestUri)
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
