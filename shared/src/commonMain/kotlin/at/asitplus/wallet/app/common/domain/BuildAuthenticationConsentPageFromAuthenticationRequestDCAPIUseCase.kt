package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import ui.navigation.routes.DCAPIAuthenticationConsentRoute
import ui.navigation.routes.DCAPIPresentationViewRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase {
    operator fun invoke(incomingRequest: DCAPIWalletRequest?): KmmResult<DCAPIPresentationViewRoute> =
        incomingRequest?.let {
            KmmResult.success(
                DCAPIPresentationViewRoute(vckJsonSerializer.encodeToString(it))
            )
        } ?: KmmResult.failure(Error("No DC API authentication request received"))
}
