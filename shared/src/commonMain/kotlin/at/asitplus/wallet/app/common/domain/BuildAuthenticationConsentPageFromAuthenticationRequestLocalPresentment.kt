package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import presentationStateModel
import ui.navigation.routes.LocalPresentationAuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment {
    operator fun invoke(incomingRequest: PresentationRequest?): KmmResult<LocalPresentationAuthenticationConsentRoute> {
        if (presentationStateModel.value == null)
            return KmmResult.failure(Error("No presentationStateModel set"))
        return incomingRequest?.let {
            KmmResult.success(
                LocalPresentationAuthenticationConsentRoute(
                    it.serialize()
                )
            )
        } ?: KmmResult.failure(Error("No presentation request received"))
    }
}