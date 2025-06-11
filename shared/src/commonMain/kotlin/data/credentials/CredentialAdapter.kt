package data.credentials

import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.signum.indispensable.io.Base64Strict
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.lib.agent.SdJwtValidator
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.Attribute
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

abstract class CredentialAdapter {
    abstract fun getAttribute(path: NormalizedJsonPath): Attribute?

    protected fun Any?.toLocalDateOrNull() =
        (this as? LocalDate?) ?: toString().let { runCatching { LocalDate.parse(it) }.getOrNull() }

    protected fun Any?.toInstantOrNull() =
        (this as? Instant?) ?: toString().let { runCatching { Instant.parse(it) }.getOrNull() }

    protected fun JsonPrimitive?.toCollectionOrNull() =
        (this as? JsonArray)?.let { it.map { it.toString() } }

    protected fun Any?.toLocalDateTimeOrNull() =
        (this as? LocalDateTime?)
            ?: toString().let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    protected fun String.toBase64UrlDecodedByteArray() = catchingUnwrapped {
        decodeToByteArray(Base64UrlStrict)
    }.getOrNull() ?: catchingUnwrapped {
        removePrefix("data:image/jpeg;base64,").decodeToByteArray(Base64Strict)
    }.getOrNull()

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

        fun getId(
            storeEntry: SubjectCredentialStore.StoreEntry
        ): String = when (storeEntry) {
            is SubjectCredentialStore.StoreEntry.Vc -> storeEntry.vc.jwtId
            is SubjectCredentialStore.StoreEntry.SdJwt -> storeEntry.sdJwt.jwtId
                ?: storeEntry.sdJwt.serialize()

            is SubjectCredentialStore.StoreEntry.Iso -> storeEntry.issuerSigned.issuerAuth.signature.humanReadableString // TODO probably not the best id
        }
    }
}