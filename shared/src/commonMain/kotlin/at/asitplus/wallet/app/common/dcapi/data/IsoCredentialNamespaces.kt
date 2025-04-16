package at.asitplus.wallet.app.common.dcapi.data

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import data.credentials.CredentialAttributeTranslator
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.jetbrains.compose.resources.getString

@Serializable
data class IsoCredentialNamespaces(
    val entry: Map<String, IsoCredentialNamespaceEntry>
) {
    fun serialize(): ByteArray = coseCompliantSerializer.encodeToByteArray(entry)

    companion object {
        fun deserialize(it: ByteArray): KmmResult<IsoCredentialNamespaces> = catching {
            coseCompliantSerializer.decodeFromByteArray(it)
        }

        suspend fun fromNamespaceAttributeMap(
            attributeMap: Map<String, Map<String, Any>>,
            attributeTranslator: CredentialAttributeTranslator
        ): Map<String, IsoCredentialNamespaces> {
            return attributeMap.map { (namespace, valuePair) ->
                namespace to IsoCredentialNamespaces(
                    valuePair.map { (name, value) ->
                        val displayName =
                            attributeTranslator.translate(name.toJsonPath())
                                ?.let { getString(it) } ?: name
                        val previewValue =
                            value.toString().safeSubstring(128) //TODO toString() is a hack
                        name to IsoCredentialNamespaceEntry(displayName, previewValue)
                    }.toMap()
                )
            }.toMap()
        }
    }
}


private fun String.toJsonPath() = NormalizedJsonPath(
    NormalizedJsonPathSegment.NameSegment(this)
)

private fun String.safeSubstring(len: Int) =
    if (this.length >= len) this.substring(0, len) + "..." else this