package data.dcapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialsContainer(
    @SerialName(CREDENTIALS)
    val credentials: List<IdentityCredentialEntry>,
) {

    companion object {
        const val CREDENTIALS = "credentials"
    }
}