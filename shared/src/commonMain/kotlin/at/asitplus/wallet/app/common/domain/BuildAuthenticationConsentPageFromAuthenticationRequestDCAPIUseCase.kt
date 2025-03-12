package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import ui.navigation.routes.DCAPIAuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase {
    operator fun invoke(incomingRequest: DCAPIRequest?): KmmResult<DCAPIAuthenticationConsentRoute> {
        return incomingRequest?.let {
            KmmResult.success(
                DCAPIAuthenticationConsentRoute(
                    it.serialize()
                )
            )
        } ?: KmmResult.failure(Error("No DCAPI authentication request received"))
    }
}