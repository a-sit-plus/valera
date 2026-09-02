package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.relyingParty.validation.WrpValidator
import ui.navigation.routes.DCAPIPresentationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase(
    val wrpValidator: WrpValidator
) {
    suspend operator fun invoke(incomingRequest: RequestParametersFrom.DcApiRequest?): KmmResult<DCAPIPresentationViewRoute> =
        incomingRequest?.let {
            val wrpValidationResult= (incomingRequest as? RequestParametersFrom<*>)?.let {
                wrpValidator.validate(incomingRequest).getOrNull()
            }
            KmmResult.success(DCAPIPresentationViewRoute(it, wrpValidationResult))
        } ?: KmmResult.failure(Error("No DC API authentication request received"))
}
