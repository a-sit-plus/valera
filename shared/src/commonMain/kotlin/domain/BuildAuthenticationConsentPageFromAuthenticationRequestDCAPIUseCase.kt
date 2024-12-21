package domain

import at.asitplus.KmmResult
import data.dcapi.DCAPIRequest
import ui.navigation.Routes.DCAPIAuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase {
    operator fun invoke(incomingRequest: DCAPIRequest?): KmmResult<DCAPIAuthenticationConsentRoute> {
        return incomingRequest?.let {
            KmmResult.success(
                DCAPIAuthenticationConsentRoute(
                    it.serialize()
                )
            )
        } ?: KmmResult.failure(Error("No API request"))
    }
}