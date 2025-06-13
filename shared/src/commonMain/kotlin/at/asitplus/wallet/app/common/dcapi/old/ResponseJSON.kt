package at.asitplus.wallet.app.common.dcapi.old

import at.asitplus.catching
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ResponseJSON(val token: String) {
    fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { vckJsonSerializer.decodeFromString<ResponseJSON>(input) }
    }
}