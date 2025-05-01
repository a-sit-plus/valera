package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.agent.SdJwtValidator
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

abstract class CredentialAdapter {
    abstract fun getAttribute(path: NormalizedJsonPath): Attribute?

    protected fun Any?.toLocalDateOrNull() =
        (this as? LocalDate?) ?: (this as? String?)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    protected fun Any?.toInstantOrNull() =
        (this as? Instant?) ?: (this as? String?)?.let { runCatching { Instant.parse(it) }.getOrNull() }

    protected fun JsonPrimitive?.toCollectionOrNull() = (this as JsonArray?)?.let { it.map { it.toString() } }

    protected fun Any?.toLocalDateTimeOrNull() =
        (this as? LocalDateTime?) ?: (this as? String?)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    abstract val representation: ConstantIndex.CredentialRepresentation

    abstract val scheme: ConstantIndex.CredentialScheme

    companion object {
        fun SubjectCredentialStore.StoreEntry.SdJwt.toAttributeMap() =
            disclosures.values.filterNotNull()
                .filter { it.claimName != null }
                .associate { it.claimName!! to it.claimValue }
                .filterValues { it is JsonPrimitive }
                .mapValues { it.value.jsonPrimitive }

        fun SubjectCredentialStore.StoreEntry.SdJwt.toComplexJson() =
            SdJwtSigned.parse(vcSerialized)?.let { SdJwtValidator(it).reconstructedJsonObject }

        fun SubjectCredentialStore.StoreEntry.Iso.toNamespaceAttributeMap() =
            issuerSigned.namespaces?.mapValues { namespace ->
                namespace.value.entries.associate {
                    it.value.elementIdentifier to it.value.elementValue
                }
            }

        @Throws(IllegalArgumentException::class)
        fun SubjectCredentialStore.StoreEntry.getDcApiId(): String = when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> vc.jwtId
            is SubjectCredentialStore.StoreEntry.SdJwt -> sdJwt.jwtId
                ?: sdJwt.subject
                ?: sdJwt.credentialStatus?.statusList?.let { it.uri.string + it.index.toString() }
                ?: throw IllegalArgumentException("Credential does not have a jwtId")
            // TODO probably not the best id
            is SubjectCredentialStore.StoreEntry.Iso -> issuerSigned.issuerAuth.signature.humanReadableString
        }
    }
}
