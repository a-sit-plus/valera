package at.asitplus.wallet.app.common.dcapi.data.export

import data.credentials.CredentialAttributeTranslator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    companion object {
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