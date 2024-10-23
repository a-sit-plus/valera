package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

sealed class CredentialAdapter {
    abstract fun getAttribute(path: NormalizedJsonPath): Attribute?

    protected fun Any?.toLocalDateOrNull() =
        (this as? LocalDate?) ?: (this as String?)?.let { LocalDate.parse(it) }

    protected fun Any?.toInstantOrNull() = (this as String?)?.let { Instant.parse(it) }

    protected fun Any?.toLocalDateTimeOrNull() =
        (this as? LocalDateTime?) ?: (this as String?)?.let { LocalDateTime.parse(it) }

    companion object {
        // Note: May need to refactor this to
        // SdJwtValidator(SdJwtSigned.parse(vcSerialized)).reconstructedJsonObject
        // to support complex SD-JWT structures
        fun SubjectCredentialStore.StoreEntry.SdJwt.toAttributeMap() =
            disclosures.values.filterNotNull()
                .filter { it.claimName != null }
                .associate { it.claimName!! to it.claimValue }
                .filterValues { it is JsonPrimitive }
                .mapValues { it.value.jsonPrimitive }

        fun SubjectCredentialStore.StoreEntry.Iso.toNamespaceAttributeMap() =
            issuerSigned.namespaces?.mapValues { namespace ->
                namespace.value.entries.associate {
                    it.value.elementIdentifier to it.value.elementValue
                }
            }
    }
}