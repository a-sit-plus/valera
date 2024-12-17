package data.dcapi

import at.asitplus.catching
import at.asitplus.wallet.lib.oidc.jsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class ResponseJSON(val token: String) {
    fun serialize(): String = jsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { jsonSerializer.decodeFromString<ResponseJSON>(input) }
    }
}
