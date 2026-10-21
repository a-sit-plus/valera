package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** A lossless matching tree. Only [displayValue] is shortened for the credential picker. */
@Serializable
data class SdJwtClaimNode(
    val kind: String,
    val value: String = "",
    val displayName: String = "",
    val displayValue: String = "",
    val members: Map<String, SdJwtClaimNode> = emptyMap(),
    val elements: List<SdJwtClaimNode> = emptyList(),
) {
    companion object {
        fun fromJson(
            json: JsonElement,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): SdJwtClaimNode = fromJson(json, NormalizedJsonPath(), "", attributeLabel)

        private fun fromJson(
            json: JsonElement,
            path: NormalizedJsonPath,
            fallbackName: String,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): SdJwtClaimNode {
            // Metadata paths commonly omit array indices; preserve the exact path for lookup first.
            val metadataPath = NormalizedJsonPath(path.filterIsInstance<NormalizedJsonPathSegment.NameSegment>())
            val displayName = attributeLabel(path)
                ?: if (metadataPath != path) attributeLabel(metadataPath) ?: fallbackName else fallbackName
            val displayValue = (if (json is JsonPrimitive) json.content else json.toString()).safeSubstring(128)
            return when (json) {
                is JsonObject -> SdJwtClaimNode(
                    kind = "object",
                    displayName = displayName,
                    displayValue = displayValue,
                    members = json.mapValues { (name, child) ->
                        fromJson(child, path + name, name, attributeLabel)
                    },
                )
                is JsonArray -> SdJwtClaimNode(
                    kind = "array",
                    displayName = displayName,
                    displayValue = displayValue,
                    elements = json.mapIndexed { index, child ->
                        fromJson(child, path + index.toUInt(), displayName, attributeLabel)
                    },
                )
                JsonNull -> SdJwtClaimNode(kind = "null", displayName = displayName, displayValue = displayValue)
                is JsonPrimitive -> SdJwtClaimNode(
                    kind = when {
                        json.isString -> "string"
                        json.booleanOrNull != null -> "boolean"
                        else -> "number"
                    },
                    value = json.content,
                    displayName = displayName,
                    displayValue = displayValue,
                )
            }
        }
    }
}
