package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.relyingParty.WrpValidator
import at.asitplus.wallet.app.common.relyingParty.toWrpValidationFailure
import io.github.aakira.napier.Napier
import ui.navigation.routes.DCAPIPresentationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase(
    val wrpValidator: WrpValidator
) {
    suspend operator fun invoke(incomingRequest: RequestParametersFrom.DcApiRequest?): KmmResult<DCAPIPresentationViewRoute> =
        incomingRequest?.let {
            val validation = (incomingRequest as? RequestParametersFrom<*>)?.let {
                wrpValidator.validate(incomingRequest).onFailure {
                    Napier.w("WRP Validation failed, continuing without registration certificate.", throwable = it)
                }
            }
            KmmResult.success(
                DCAPIPresentationViewRoute(
                    request = it,
                    wrpValidationResult = validation?.getOrNull(),
                    wrpValidationFailure = validation?.exceptionOrNull()?.toWrpValidationFailure(),
                )
            )
        } ?: KmmResult.failure(Error("No DC API authentication request received"))
}
