package domain

import Resources
import at.asitplus.wallet.lib.oidc.RelyingPartyMetadata
import io.github.aakira.napier.Napier

class ValidateClientIdConsistencyWithClientMetadataUseCase {

    operator fun invoke(
        clientId: String,
        clientMetadataPayload: RelyingPartyMetadata
    ) {
        if (!clientMetadataPayload.redirectUris.contains(clientId)) {
            val redirectUris = clientMetadataPayload.redirectUris.joinToString("\n - ")
            val message =
                "${Resources.ERROR_QR_CODE_SCANNING_CLIENT_ID_NOT_IN_REDICECT_URIS}:" +
                        " ${clientId} not in: \n$redirectUris)"
            throw Exception(message)
        } else {
            Napier.d("Valid client id: ${clientId}")
        }
    }
}