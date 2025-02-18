package at.asitplus.wallet.app.common.presentation

import at.asitplus.catching
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class PresentationRequest(
    val type: String
) {

    fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { vckJsonSerializer.decodeFromString<PresentationRequest>(input) }
    }
}


