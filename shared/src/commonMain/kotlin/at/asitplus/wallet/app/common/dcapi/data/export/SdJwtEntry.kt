package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        fun fromAttributeList(
            attributeList: List<Pair<NormalizedJsonPath, Any>>,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): Map<String, ExportedElements> = attributeList.map { (path, value) ->
            val name = path.toDcqlPathKey()
            val displayName = attributeLabel(path) ?: name
            val truncatedValue = value.toCustomString().safeSubstring(128)
            name to ExportedElements(displayName, truncatedValue, truncatedValue)
        }.toMap()
    }
}
