package domain

import at.asitplus.KmmResult
import at.asitplus.dcapi.request.Oid4vpDCAPIRequest
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import ui.navigation.routes.AuthenticationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
    val presentationService: PresentationService,
) {
    suspend operator fun invoke(requestUri: String, incomingRequest: Oid4vpDCAPIRequest? = null): KmmResult<AuthenticationViewRoute> {
        val request = presentationService.parseAuthenticationRequestParameters(requestUri).getOrElse {
            Napier.d("authenticationRequestParameters: $it")
            return KmmResult.failure(it)
        }

        val preparationState = presentationService.startAuthorizationResponsePreparation(request).getOrElse {
            Napier.e("Failure", it)
            return KmmResult.failure(it)
        }

        // TODO: extract recipient name from the metadataResponse; the data is not yet being delivered though
        return KmmResult.success(
            AuthenticationViewRoute(
                authenticationRequestParametersFromSerialized = vckJsonSerializer.encodeToString(request),
                authorizationPreparationStateSerialized = vckJsonSerializer.encodeToString(preparationState),
                recipientLocation = request.parameters.clientId ?: "",
                isCrossDeviceFlow = false,
                oid4vpDCAPIRequestSerialized = incomingRequest?.let {
                    vckJsonSerializer.encodeToString(it)
                }
            )
        )
    }
}