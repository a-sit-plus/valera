package at.asitplus.wallet.app.common.dcapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialField(
    @SerialName(FORMAT)
    val format: String,
    @SerialName(DISPLAY_INFO)
    val displayInfo: DisplayInfoField,
    @SerialName(FIELDS)
    val fields: List<IdentityCredentialField>,
) {

    companion object {
        const val FORMAT = "format"
        const val DISPLAY_INFO = "display_info"
        const val FIELDS = "fields"
    }
}