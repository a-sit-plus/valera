package at.asitplus.wallet.app.common.dcapi.data.request

import at.asitplus.catching
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Oid4vpDCAPIRequest(
    val protocol: String,
    val request: String,
    val credentialId: Int,
    val callingPackageName: String?,
    val callingOrigin: String?
) : DCAPIRequest() {
    init {
        require(callingOrigin != null || callingPackageName != null)
    }

    override fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) =
            catching { vckJsonSerializer.decodeFromString<Oid4vpDCAPIRequest>(input) }
    }
}
