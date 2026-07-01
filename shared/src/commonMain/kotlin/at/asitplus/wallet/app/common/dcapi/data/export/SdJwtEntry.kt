package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class SdJwtEntry(
    @SerialName("documentId")
    val jwtId: String,
    @SerialName("vct")
    val verifiableCredentialType: String,
    @SerialName("claims")
    val claims: Map<String, ExportedElements>
) {
    companion object {
        suspend fun fromAttributeMap(
            attributeMap: Map<String, JsonPrimitive>,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): Map<String, ExportedElements> = attributeMap.map { (name, value) ->
            val displayName = attributeLabel(name.toJsonPath()) ?: name
            val truncatedValue = value.toCustomString().safeSubstring(128)
            name to ExportedElements(displayName, truncatedValue, truncatedValue)
        }.toMap()
    }
}
