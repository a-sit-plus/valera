package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IsoMdocEntry(
    @SerialName("documentId")
    val id: String,
    @SerialName("docType")
    val docType: String,
    @SerialName("namespaces")
    val isoNamespaces: Map<String, Map<String, ExportedElements>>
) {

    companion object {
        suspend fun isoNamespacesFromNamespaceAttributeMap(
            attributeMap: Map<String, Map<String, Any>>,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): Map<String, Map<String, ExportedElements>> {
            return attributeMap.map { (namespace, valuePair) ->
                namespace to valuePair.map { (name, value) ->
                    val displayName =
                        attributeLabel(name.toJsonPath()) ?: name
                    val truncatedValue = value.toCustomString().safeSubstring(128)
                    name to ExportedElements(displayName, truncatedValue, truncatedValue)
                }.toMap()
            }.toMap()
        }
    }
}
