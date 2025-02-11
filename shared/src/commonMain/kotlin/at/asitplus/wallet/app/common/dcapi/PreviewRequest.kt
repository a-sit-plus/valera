package at.asitplus.wallet.app.common.dcapi

import at.asitplus.catching
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

@Serializable
data class PreviewRequest(
    val selector: JsonObject,
    val nonce: String,
    @SerialName("readerPublicKey")
    val readerPublicKeyBase64: String
) {
    fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { vckJsonSerializer.decodeFromString<PreviewRequest>(input) }
    }
}