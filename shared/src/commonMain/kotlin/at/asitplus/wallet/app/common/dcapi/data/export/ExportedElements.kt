package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Serializable
@CborArray
data class ExportedElements(
    val elementFriendlyName: String,
    val elementValue: String
) {
    fun serialize(): ByteArray = coseCompliantSerializer.encodeToByteArray(this)

    companion object {
        fun deserialize(it: ByteArray): KmmResult<ExportedElements> = catching {
            coseCompliantSerializer.decodeFromByteArray(it)
        }
    }
}