package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray

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
        /**
         * Exports claims under the keys the Android DC API matcher builds from DCQL claim paths: path segments joined
         * with `.`, where an array index is its decimal string and the `null` wildcard is `null`. Array claims are
         * therefore exported under their own key, per element (`nationalities.0`) and as a wildcard
         * (`nationalities.null`), and arrays of objects additionally as projections (`address.null.country`).
         */
        fun fromAttributeList(
            attributeList: List<Pair<NormalizedJsonPath, Any>>,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): Map<String, ExportedElements> = buildMap {
            attributeList.forEach { (path, value) ->
                val name = path.toDcqlPathKey()
                val displayName = attributeLabel(path) ?: name
                val truncatedValue = value.toCustomString().safeSubstring(128)
                put(name, ExportedElements(displayName, truncatedValue, truncatedValue))
                if (value is JsonElement) {
                    putSubClaims(name, path, value, displayName, attributeLabel)
                }
            }
        }

        private fun MutableMap<String, ExportedElements>.putSubClaims(
            key: String,
            path: NormalizedJsonPath,
            value: JsonElement,
            displayName: String,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ) {
            when (value) {
                is JsonObject -> value.forEach { (member, memberValue) ->
                    val memberPath = path + member
                    val memberDisplayName = attributeLabel(memberPath) ?: member
                    putClaim("$key.$member", memberDisplayName, memberValue)
                    putSubClaims("$key.$member", memberPath, memberValue, memberDisplayName, attributeLabel)
                }

                is JsonArray -> {
                    value.forEachIndexed { index, element ->
                        // NormalizedJsonPath has no wildcard, so labels of nested members ignore the array step
                        putClaim("$key.$index", displayName, element)
                        putSubClaims("$key.$index", path, element, displayName, attributeLabel)
                    }
                    if (value.isNotEmpty()) {
                        // a wildcard selects all elements, which has no single value to compare against
                        put("$key.null", ExportedElements(displayName, value.toString().safeSubstring(128), ""))
                        putWildcardProjections("$key.null", path, value, attributeLabel)
                    }
                }

                else -> {}
            }
        }

        private fun MutableMap<String, ExportedElements>.putWildcardProjections(
            key: String,
            path: NormalizedJsonPath,
            selection: JsonArray,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ) {
            val members = selection.filterIsInstance<JsonObject>().flatMap { it.keys }.toSet()
            members.forEach { member ->
                val projected = buildJsonArray {
                    selection.filterIsInstance<JsonObject>().forEach { element -> element[member]?.let { add(it) } }
                }
                val memberPath = path + member
                val memberDisplayName = attributeLabel(memberPath) ?: member
                put("$key.$member", ExportedElements(memberDisplayName, projected.toString().safeSubstring(128), ""))
                putWildcardProjections("$key.$member", memberPath, projected, attributeLabel)
            }
        }

        private fun MutableMap<String, ExportedElements>.putClaim(key: String, displayName: String, value: JsonElement) {
            val truncatedValue = value.toCustomString().safeSubstring(128)
            put(key, ExportedElements(displayName, truncatedValue, truncatedValue))
        }
    }
}
