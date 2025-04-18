package at.asitplus.wallet.app.common.domain

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.dcapi.data.DCAPIRequest
import at.asitplus.wallet.app.common.dcapi.data.preview.PreviewDCAPIRequest
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