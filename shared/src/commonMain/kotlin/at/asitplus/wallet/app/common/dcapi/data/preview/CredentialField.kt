package at.asitplus.wallet.app.common.dcapi.data.preview

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Format as defined in the documentation https://digitalcredentials.dev/docs/wallets/android
// and sample code: https://github.com/openwallet-foundation-labs/identity-credential/tree/main/wallet/src/main/java/com/android/identity_credential/wallet/credman
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