package at.asitplus.wallet.app.common.dcapi.data.preview

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PreviewRequest(
    val selector: JsonObject,
    val nonce: String,
    @SerialName("readerPublicKey")
    val readerPublicKeyBase64: String
)