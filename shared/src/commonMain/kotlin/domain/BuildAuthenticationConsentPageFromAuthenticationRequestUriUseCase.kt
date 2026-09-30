package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.relyingParty.WrpValidator
import at.asitplus.wallet.app.common.relyingParty.toWrpValidationFailure
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
    val wrpValidator: WrpValidator
) {
    suspend operator fun invoke(
        requestUri: String,
    ): KmmResult<AuthenticationViewRoute> = catching {
        val preparationState = presentationService.startAuthorizationResponsePreparation(requestUri)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()
        val validation = wrpValidator.validate(preparationState.request).onFailure {
            Napier.w("WRP Validation failed, continuing without registration certificate.", throwable = it)
        }

        AuthenticationViewRoute(
            authenticationRequest = preparationState.request,
            authorizationResponsePreparationState = preparationState,
            recipientLocation = preparationState.request.parameters.clientId ?: "",
            isCrossDeviceFlow = false,
            wrpValidationResult = validation.getOrNull(),
            wrpValidationFailure = validation.exceptionOrNull()?.toWrpValidationFailure(),

        )
    }
}
