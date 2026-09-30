package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.relyingParty.WrpValidator
import io.github.aakira.napier.Napier
import ui.navigation.routes.DCAPIPresentationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase(
    val wrpValidator: WrpValidator
) {
    suspend operator fun invoke(incomingRequest: RequestParametersFrom.DcApiRequest?): KmmResult<DCAPIPresentationViewRoute> =
        incomingRequest?.let {
            val wrpValidationResult= (incomingRequest as? RequestParametersFrom<*>)?.let {
                wrpValidator.validate(incomingRequest).getOrElse {
                    Napier.w("WRP Validation failed, continuing without registration certificate.", throwable = it)
                    null
                }
            }
            KmmResult.success(DCAPIPresentationViewRoute(it, wrpValidationResult))
        } ?: KmmResult.failure(Error("No DC API authentication request received"))
}
