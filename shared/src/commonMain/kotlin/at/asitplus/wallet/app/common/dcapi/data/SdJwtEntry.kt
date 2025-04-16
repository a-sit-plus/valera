package at.asitplus.wallet.app.common.dcapi.data

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Serializable
data class SdJwtEntry(
    @SerialName("todo")
    val todo: String
) {
    fun serialize(): ByteArray = coseCompliantSerializer.encodeToByteArray(this)

    companion object {
        fun deserialize(it: ByteArray): KmmResult<SdJwtEntry> = catching {
            coseCompliantSerializer.decodeFromByteArray(it)
        }
    }
}