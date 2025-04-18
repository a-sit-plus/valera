package at.asitplus.wallet.app.common.dcapi.data.oid4vp

import at.asitplus.catching
import at.asitplus.wallet.app.common.dcapi.data.DCAPIRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Oid4vpDCAPIRequest(val request: String) : DCAPIRequest() {
    override fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { vckJsonSerializer.decodeFromString<Oid4vpDCAPIRequest>(input) }
    }
}
