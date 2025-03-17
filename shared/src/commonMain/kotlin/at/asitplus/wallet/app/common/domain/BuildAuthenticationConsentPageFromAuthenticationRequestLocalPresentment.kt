package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import presentationStateModel
import ui.navigation.routes.LocalPresentationAuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment {
    operator fun invoke(incomingRequest: PresentationRequest?): KmmResult<LocalPresentationAuthenticationConsentRoute> =
        catching {
            require(presentationStateModel.value != null) { "No presentationStateModel set" }
            require(incomingRequest != null) { "No presentation request received" }
            LocalPresentationAuthenticationConsentRoute(
                incomingRequest.serialize()
            )
        }
}