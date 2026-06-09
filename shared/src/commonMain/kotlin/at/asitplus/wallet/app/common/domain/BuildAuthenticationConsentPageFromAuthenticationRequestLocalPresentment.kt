package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.catching
import ui.navigation.routes.LocalPresentationAuthenticationConsentRoute
import ui.viewmodels.authentication.PresentationStateModel

class BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment {
    operator fun invoke(
        presentationStateModel: PresentationStateModel?
    ): KmmResult<LocalPresentationAuthenticationConsentRoute> =
        catching {
            require(presentationStateModel != null) { "No presentationStateModel set" }
            LocalPresentationAuthenticationConsentRoute
        }
}
