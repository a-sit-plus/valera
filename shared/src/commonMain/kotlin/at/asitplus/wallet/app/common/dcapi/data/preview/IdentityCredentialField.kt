package at.asitplus.wallet.app.common.dcapi.data.preview

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import data.credentials.CredentialAttributeTranslator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.getString

// Format as defined in the documentation https://digitalcredentials.dev/docs/wallets/android
// and sample code: https://github.com/openwallet-foundation-labs/identity-credential/tree/main/wallet/src/main/java/com/android/identity_credential/wallet/credman
@Serializable
data class IdentityCredentialField(
    @SerialName(NAME)
    val name: String,
    @SerialName(VALUE)
    val value: String?, // TODO this should be Any
    @SerialName(DISPLAY_NAME)
    val displayName: String,
    @SerialName(DISPLAY_VALUE)
    val displayValue: String?,
) {

    companion object {
        const val NAME = "name"
        const val VALUE = "value"
        const val DISPLAY_NAME = "display_name"
        const val DISPLAY_VALUE = "display_value"

        suspend fun fromNamespaceAttributeMap(
            attributeMap: Map<String, Map<String, Any>>,
            attributeTranslator: CredentialAttributeTranslator
        ): List<IdentityCredentialField> = attributeMap.flatMap { (namespace, valuePair) ->
            valuePair.map { (name, value) ->
                val entryName = "$namespace.$name"
                val displayName = attributeTranslator.translate(name.toJsonPath())?.let { getString(it) } ?: name
                val serializedValue = value.toString().safeSubstring(128) //TODO toString() is a hack
                IdentityCredentialField(entryName, serializedValue, displayName, serializedValue)
            }
        }

        private fun String.toJsonPath() = NormalizedJsonPath(
            NormalizedJsonPathSegment.NameSegment(this)
        )

        // TODO untested
        suspend fun fromAttributeMap(
            attributeMap: Map<String, JsonPrimitive>,
            attributeTranslator: CredentialAttributeTranslator
        ): List<IdentityCredentialField> = attributeMap.map { (name, value) ->
            val displayName = attributeTranslator.translate(name.toJsonPath())?.let { getString(it) } ?: name
            val serializedValue = value.toString().safeSubstring(128) //TODO toString() is a hack
            IdentityCredentialField(name, serializedValue, displayName, serializedValue)
        }


    }
}

private fun String.safeSubstring(len: Int) = if (this.length >= len) this.substring(0, len) + "..." else this
