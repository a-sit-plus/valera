package at.asitplus.wallet.app.common.dcapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// Format as defined in the documentation https://digitalcredentials.dev/docs/wallets/android
// and sample code: https://github.com/openwallet-foundation-labs/identity-credential/tree/main/wallet/src/main/java/com/android/identity_credential/wallet/credman
@Serializable
data class IdentityCredentialEntry(
    @SerialName(ID)
    val id: Int,
    @SerialName(CREDENTIAL)
    val credential: CredentialField,
    @Transient
    val icon: ByteArray? = null
) {

    companion object {
        const val ID = "id"
        const val CREDENTIAL = "credential"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IdentityCredentialEntry

        if (id != other.id) return false
        if (credential != other.credential) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + credential.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}