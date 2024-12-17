package data.dcapi

import data.credentials.CredentialAttributeTranslator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

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

        fun fromNamespaceAttributeMap(attributeMap: Map<String, Map<String, Any>>?, attributeTranslator: CredentialAttributeTranslator?): List<IdentityCredentialField> {
            val entries = mutableListOf<IdentityCredentialField>()
            attributeMap?.forEach { (namespace, valuePair) ->
                valuePair.forEach { (name, value) ->
                    val entryName = "$namespace.$name"
                    val displayName = name //stringResource(attributeTranslator?.translate("")) ?: name

                    entries.add(IdentityCredentialField(entryName, value.toString(), displayName, value.toString())) //TODO toString() is a hack
                }
            }
            return entries
        }

        fun fromAttributeMap(attributeMap: Map<String, JsonPrimitive>, attributeTranslator: CredentialAttributeTranslator?): List<IdentityCredentialField> {
            // TODO untested
            val entries = mutableListOf<IdentityCredentialField>()
            attributeMap.forEach { (name, value) ->
                    val entryName = "$name"
                    entries.add(IdentityCredentialField(entryName, value.toString(), name, value.toString())) //TODO toString() is a hack
                }
            return entries
        }


    }
}