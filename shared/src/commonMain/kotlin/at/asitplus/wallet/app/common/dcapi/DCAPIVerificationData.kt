package at.asitplus.wallet.app.common.dcapi

import at.asitplus.openid.RequestParametersFrom
import kotlinx.serialization.Serializable

@Serializable
enum class DCAPICredentialRepresentation {
    ISO_MDOC,
    SD_JWT,
}

@Serializable
data class DCAPICredentialType(
    val representation: DCAPICredentialRepresentation,
    val type: String,
)

sealed interface DCAPIVerificationData {
    data class Presentation(
        val request: RequestParametersFrom.DcApiRequest,
    ) : DCAPIVerificationData

    data class IssuanceRequired(
        val credentialType: DCAPICredentialType,
    ) : DCAPIVerificationData
}
