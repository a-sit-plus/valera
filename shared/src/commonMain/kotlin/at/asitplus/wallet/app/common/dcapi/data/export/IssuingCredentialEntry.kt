package at.asitplus.wallet.app.common.dcapi.data.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssuingCredentialEntry(
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String,
    @SerialName("documentId")
    val id: String,
    @SerialName("mdocDocTypes")
    val mdocDocTypes: List<String>,
    @SerialName("sdJwtVcts")
    val sdJwtVcts: List<String>,
)
