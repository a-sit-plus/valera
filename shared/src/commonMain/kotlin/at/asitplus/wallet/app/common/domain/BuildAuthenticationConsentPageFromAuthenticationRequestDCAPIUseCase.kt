package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.openid.RequestParametersFrom
import ui.navigation.routes.DCAPIPresentationViewRoute

/** The request is validated when it is prepared, see [ui.presentation.DCAPIPresentationGraphViewModel]. */
class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase {
    operator fun invoke(incomingRequest: RequestParametersFrom.DcApiRequest?): KmmResult<DCAPIPresentationViewRoute> =
        incomingRequest?.let {
            KmmResult.success(DCAPIPresentationViewRoute(it))
        } ?: KmmResult.failure(Error("No DC API authentication request received"))
}
