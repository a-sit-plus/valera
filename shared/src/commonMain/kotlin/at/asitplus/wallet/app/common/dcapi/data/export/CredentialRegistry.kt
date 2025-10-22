package at.asitplus.wallet.app.common.dcapi.data.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class CredentialRegistry private constructor(
    @SerialName("protocols")
    val protocols: List<String>,
    @SerialName("credentials")
    val credentials: List<CredentialEntry>
) {
    companion object {
        fun create(credentials: List<CredentialEntry>): CredentialRegistry = CredentialRegistry(
            listOf(
                "openid4vp-v1-signed",
                "openid4vp-v1-unsigned",
                "org-iso-mdoc",
                "openid4vp"
            ), credentials
        )
    }
}