package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import ui.navigation.routes.LocalPresentationAuthenticationConsentRoute
import ui.viewmodels.authentication.PresentationStateModel

class BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment {
    operator fun invoke(
        incomingRequest: PresentationRequest?,
        presentationStateModel: PresentationStateModel?
    ): KmmResult<LocalPresentationAuthenticationConsentRoute> =
        catching {
            require(presentationStateModel != null) { "No presentationStateModel set" }
            require(incomingRequest != null) { "No presentation request received" }
            LocalPresentationAuthenticationConsentRoute(incomingRequest)
        }
}
