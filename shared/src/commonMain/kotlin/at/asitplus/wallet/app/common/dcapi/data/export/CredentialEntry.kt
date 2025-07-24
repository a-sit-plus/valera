package at.asitplus.wallet.app.common.dcapi.data.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.ByteString

@Serializable
data class CredentialEntry(
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String,
    @ByteString
    @SerialName("bitmap")
    val bitmap: ByteArray?,
    @SerialName("mdoc")
    val isoEntry: IsoEntry? = null,
    @SerialName("sdjwt")
    val sdJwtEntry: SdJwtEntry? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CredentialEntry

        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (bitmap != null) {
            if (other.bitmap == null) return false
            if (!bitmap.contentEquals(other.bitmap)) return false
        } else if (other.bitmap != null) return false
        if (isoEntry != other.isoEntry) return false
        if (sdJwtEntry != other.sdJwtEntry) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + subtitle.hashCode()
        result = 31 * result + (bitmap?.contentHashCode() ?: 0)
        result = 31 * result + (isoEntry?.hashCode() ?: 0)
        result = 31 * result + (sdJwtEntry?.hashCode() ?: 0)
        return result
    }
}