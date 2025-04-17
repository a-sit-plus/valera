package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import data.credentials.CredentialAttributeTranslator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.getString

@Serializable
data class SdJwtEntry(
    @SerialName("id")
    val jwtId: String,
    @SerialName("vct")
    val verifiableCredentialType: String,
    @SerialName("claims")
    val claims: Map<String, ExportedElements>
) {
    fun serialize(): ByteArray = coseCompliantSerializer.encodeToByteArray(this)

    companion object {
        fun deserialize(it: ByteArray): KmmResult<SdJwtEntry> = catching {
            coseCompliantSerializer.decodeFromByteArray(it)
        }

        suspend fun fromAttributeMap(
            attributeMap: Map<String, JsonPrimitive>,
            attributeTranslator: CredentialAttributeTranslator
        ): Map<String, ExportedElements> = attributeMap.map { (name, value) ->
            val displayName = attributeTranslator.translate(name.toJsonPath())?.let { getString(it) } ?: name
            val previewValue = value.toCustomString().safeSubstring(128)
            name to ExportedElements(displayName, previewValue)
        }.toMap()
    }
}