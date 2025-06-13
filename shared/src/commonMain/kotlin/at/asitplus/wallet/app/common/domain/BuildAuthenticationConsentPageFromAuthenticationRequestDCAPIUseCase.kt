package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.dcapi.request.DCAPIRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import ui.navigation.routes.DCAPIAuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase {
    operator fun invoke(incomingRequest: DCAPIRequest?): KmmResult<DCAPIAuthenticationConsentRoute> =
        incomingRequest?.let {
            KmmResult.success(
                DCAPIAuthenticationConsentRoute(vckJsonSerializer.encodeToString(it))
            )
        } ?: KmmResult.failure(Error("No DCAPI authentication request received"))
}
