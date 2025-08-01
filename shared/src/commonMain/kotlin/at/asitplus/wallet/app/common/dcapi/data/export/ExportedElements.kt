package at.asitplus.wallet.app.common.dcapi.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray

@Serializable
@CborArray
data class ExportedElements(
    val elementFriendlyName: String,
    val elementValue: String
)