package domain

import at.asitplus.KmmResult
import data.dcapi.DCAPIRequest
import ui.navigation.Routes.APIAuthenticationConsentRoute

class BuildAuthenticationConsentPageFromAuthenticationRequestAPIUseCase {
    operator fun invoke(incomingRequest: DCAPIRequest?): KmmResult<APIAuthenticationConsentRoute> {
        return incomingRequest?.let {
            KmmResult.success(
                APIAuthenticationConsentRoute(
                    it.serialize()
                )
            )
        } ?: KmmResult.failure(Error("No API request"))
    }
}