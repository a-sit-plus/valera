package data.credentials

import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.signum.indispensable.io.Base64Strict
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.lib.agent.SdJwtDecoded
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.LocalDateOrInstant
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.Attribute
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

abstract class CredentialAdapter {
    abstract fun getAttribute(path: NormalizedJsonPath): Attribute?

    protected fun Any?.toLocalDateOrNull() =
        (this as? LocalDate?) ?: toString().let { runCatching { LocalDate.parse(it) }.getOrNull() }

    protected fun Any?.toInstantOrNull() =
        (this as? Instant?) ?: toString().let { runCatching { Instant.parse(it) }.getOrNull() }

    protected fun Any?.toLocalDateOrInstantOrNull() = (this as? LocalDateOrInstant?)
        ?: (this as? LocalDateOrInstant.LocalDate?)
        ?: (this as? LocalDateOrInstant.Instant?)
        ?: toString().let {
            runCatching { Instant.parse(it) }.getOrNull()
                ?.let { LocalDateOrInstant.Instant(it) }
        }
        ?: toString().let {
            runCatching { LocalDate.parse(it) }.getOrNull()
                ?.let { LocalDateOrInstant.LocalDate(it) }
        }

    protected fun JsonPrimitive?.toCollectionOrNull() =
        (this as? JsonArray)?.let { it.map { it.toString() } }

    protected fun Any?.toLocalDateTimeOrNull() =
        (this as? LocalDateTime?)
            ?: toString().let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    protected fun String.decodeFromPortraitString() = catchingUnwrapped {
        decodeToByteArray(Base16) // e.g. from demo.wallet-gw.namirial.com
    }.getOrNull() ?: catchingUnwrapped {
        decodeToByteArray(Base64UrlStrict)
    }.getOrNull() ?: catchingUnwrapped {
        // e.g. from demo-issuer.wwwallet.org/
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
            SdJwtSigned.parse(vcSerialized)?.let { SdJwtDecoded(it).reconstructedJsonObject }

        fun SubjectCredentialStore.StoreEntry.Iso.toNamespaceAttributeMap() =
            issuerSigned.namespaces?.mapValues { namespace ->
                namespace.value.entries.associate {
                    it.value.elementIdentifier to it.value.elementValue
                }
            }
    }
}
