package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.relyingParty.WrpValidator
import ui.navigation.routes.DCAPIPresentationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase(
    val wrpValidator: WrpValidator
) {
    suspend operator fun invoke(incomingRequest: RequestParametersFrom.DcApiRequest?): KmmResult<DCAPIPresentationViewRoute> =
        incomingRequest?.let {
            val wrpValidationResult = (it as? RequestParametersFrom<*>)?.let { request -> wrpValidator.validate(request) }
            KmmResult.success(DCAPIPresentationViewRoute(it, wrpValidationResult))
        } ?: KmmResult.failure(Error("No DC API authentication request received"))
}
