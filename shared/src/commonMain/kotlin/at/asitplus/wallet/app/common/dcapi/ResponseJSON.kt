package at.asitplus.wallet.app.common.dcapi

import at.asitplus.catching
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class ResponseJSON(val token: String) {
    fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { vckJsonSerializer.decodeFromString<ResponseJSON>(input) }
    }
}
