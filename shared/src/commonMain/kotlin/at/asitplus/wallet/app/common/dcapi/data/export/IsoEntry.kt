package at.asitplus.wallet.app.common.dcapi.data.export

import data.credentials.CredentialAttributeTranslator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString

@Serializable
data class IsoEntry(
    @SerialName("id")
    val id: String,
    @SerialName("docType")
    val docType: String,
    @SerialName("namespaces")
    val isoNamespaces: Map<String, Map<String, ExportedElements>>
) {

    companion object {
        suspend fun isoNamespacesFromNamespaceAttributeMap(
            attributeMap: Map<String, Map<String, Any>>,
            attributeTranslator: CredentialAttributeTranslator
        ): Map<String, Map<String, ExportedElements>> {
            return attributeMap.map { (namespace, valuePair) ->
                namespace to valuePair.map { (name, value) ->
                    val displayName =
                        attributeTranslator.translate(name.toJsonPath())
                            ?.let { getString(it) } ?: name
                    val previewValue =
                        value.toCustomString().safeSubstring(128)
                    name to ExportedElements(displayName, previewValue)
                }.toMap()
            }.toMap()
        }
    }
}