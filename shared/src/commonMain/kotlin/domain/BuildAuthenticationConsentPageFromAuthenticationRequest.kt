package domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.domain.requestcertificates.RequestCertificateValidator
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequest(
    val presentationService: PresentationService,
    httpService: HttpService,
) {
    private val requestCertificateValidator = RequestCertificateValidator(httpService)

    suspend operator fun invoke(
        request: RequestParametersFrom<AuthenticationRequestParameters>,
    ): KmmResult<AuthenticationViewRoute> = catching {
        Napier.d("BuildAuthenticationConsentPageFromAuthenticationRequest: request=$request")
        val preparationState = presentationService.startAuthorizationResponsePreparation(request)
            .onFailure { Napier.e("Failure", it) }
            .getOrThrow()
        val validationResult = requestCertificateValidator.validate(preparationState)
        val recipientDisplay = validationResult.preferredRecipientDisplay()
            ?: preparationState.request.parameters.clientId
            ?: ""
        Napier.d(
            "BuildAuthenticationConsentPageFromAuthenticationRequest: " +
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
