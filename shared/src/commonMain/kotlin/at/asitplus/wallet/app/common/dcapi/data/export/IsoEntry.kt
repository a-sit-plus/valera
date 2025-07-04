package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.cosef.io.Base16Strict
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import data.credentials.CredentialAttributeTranslator
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
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
    fun serialize(): ByteArray = coseCompliantSerializer.encodeToByteArray(this)

    companion object {
        fun deserialize(it: ByteArray): KmmResult<IsoEntry> = catching {
            coseCompliantSerializer.decodeFromByteArray(it)
        }

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