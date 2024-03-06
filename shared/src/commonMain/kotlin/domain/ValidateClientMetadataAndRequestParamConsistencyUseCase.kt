package domain

import Resources
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.RelyingPartyMetadata
import io.github.aakira.napier.Napier

class ValidateClientMetadataAndRequestParameterConsistencyUseCase {

    operator fun invoke(authenticationRequestParameters: AuthenticationRequestParameters, clientMetadataPayload: RelyingPartyMetadata) {
        if (!clientMetadataPayload.redirectUris.contains(authenticationRequestParameters.clientId)) {
            val redirectUris = clientMetadataPayload.redirectUris.joinToString("\n - ")
            val message =
                "${Resources.ERROR_QR_CODE_SCANNING_CLIENT_ID_NOT_IN_REDICECT_URIS}:" +
                        " ${authenticationRequestParameters.clientId} not in: \n$redirectUris)"
            throw Exception(message)
        } else {
            Napier.d("Valid client id: ${authenticationRequestParameters.clientId}")
        }
    }
}