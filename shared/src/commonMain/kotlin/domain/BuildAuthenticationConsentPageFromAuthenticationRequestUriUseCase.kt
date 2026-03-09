package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.domain.requestcertificates.RequestCertificateValidator
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
    httpService: HttpService,
) {
    private val requestCertificateValidator = RequestCertificateValidator(httpService)

    suspend operator fun invoke(
        requestUri: String,
    ): KmmResult<AuthenticationViewRoute> = catching {
        Napier.d("BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase: requestUri=$requestUri")
        val preparationState = presentationService.startAuthorizationResponsePreparation(requestUri)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()
        val validationResult = requestCertificateValidator.validate(preparationState)
        val recipientDisplay = validationResult.preferredRecipientDisplay()
            ?: preparationState.request.parameters.clientId
            ?: ""
        Napier.d(
            "BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase: " +
                    "registrationCertPayloads=${validationResult.registrationCertPayloads.size}, " +
                    "recipientDisplay='$recipientDisplay'"
        )

        AuthenticationViewRoute(
            authenticationRequest = preparationState.request,
            authorizationResponsePreparationState = preparationState,
            recipientLocation = recipientDisplay,
            isCrossDeviceFlow = false,
        )
    }
}
