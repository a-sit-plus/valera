package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute
import io.github.aakira.napier.Napier
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

sealed class IdAustriaCredentialAdapter(
    private val decodePortrait: (ByteArray) -> ImageBitmap?,
) : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        with(IdAustriaScheme.Attributes) {
            when (first) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                    BPK -> Attribute.fromValue(bpk)
                    FIRSTNAME -> Attribute.fromValue(givenName)
                    LASTNAME -> Attribute.fromValue(familyName)
                    DATE_OF_BIRTH -> Attribute.fromValue(dateOfBirth)
                    PORTRAIT -> Attribute.fromValue(portraitBitmap)
                    AGE_OVER_14 -> Attribute.fromValue(ageAtLeast14)
                    AGE_OVER_16 -> Attribute.fromValue(ageAtLeast16)
                    AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                    AGE_OVER_21 -> Attribute.fromValue(ageAtLeast21)
                    MAIN_ADDRESS -> when (val second =
                        path.segments.getOrNull(1)) {
                        is NormalizedJsonPathSegment.NameSegment ->
                            with(IdAustriaCredentialMainAddress) {
                                when (second.memberName) {
                                    GEMEINDEKENNZIFFER -> Attribute.fromValue(mainAddress?.municipalityCode)
                                    GEMEINDEBEZEICHNUNG -> Attribute.fromValue(mainAddress?.municipalityName)
                                    POSTLEITZAHL -> Attribute.fromValue(mainAddress?.postalCode)
                                    ORTSCHAFT -> Attribute.fromValue(mainAddress?.locality)
                                    STRASSE -> Attribute.fromValue(mainAddress?.street)
                                    HAUSNUMMER -> Attribute.fromValue(mainAddress?.houseNumber)
                                    STIEGE -> Attribute.fromValue(mainAddress?.stair)
                                    TUER -> Attribute.fromValue(mainAddress?.door)
                                    else -> null
                                }
                            }

                        null -> Attribute.fromValue(mainAddressRaw)

                        else -> null
                    }

                    else -> null
                }

                else -> null
            }
        }
    }

    abstract val bpk: String?
    abstract val givenName: String?
    abstract val familyName: String?
    abstract val dateOfBirth: LocalDate?
    abstract val portraitRaw: ByteArray?
    val portraitBitmap: ImageBitmap? by lazy {

        kotlin.runCatching {
            portraitRaw?.let(decodePortrait)
        }.onFailure { Napier.e(throwable = it) { "Error decoding image" } }.getOrNull()
    }
    abstract val ageAtLeast14: Boolean?
    abstract val ageAtLeast16: Boolean?
    abstract val ageAtLeast18: Boolean?
    abstract val ageAtLeast21: Boolean?
    val mainAddress by lazy {
        mainAddressRaw?.let {
            Json.decodeFromString<IdAustriaCredentialMainAddress>(it.decodeBase64String())
        }
    }
    abstract val mainAddressRaw: String?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
            decodeImage: (ByteArray) -> ImageBitmap?,
        ): IdAustriaCredentialAdapter {
            if (storeEntry.scheme !is IdAustriaScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    (storeEntry.vc.vc.credentialSubject as? IdAustriaCredential)?.let {
                        IdAustriaCredentialVcAdapter(it, decodeImage = decodeImage)
                    } ?: throw IllegalArgumentException("credential")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    IdAustriaCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                        decodeImage = decodeImage,
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    IdAustriaCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                        decodeImage = decodeImage,
                    )
                }
            }
        }
    }
}

private class IdAustriaCredentialVcAdapter(
    val credentialSubject: IdAustriaCredential,
    decodeImage: (ByteArray) -> ImageBitmap?,
) : IdAustriaCredentialAdapter(decodeImage) {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.PLAIN_JWT

    override val bpk: String
        get() = credentialSubject.bpk

    override val givenName: String
        get() = credentialSubject.firstname

    override val familyName: String
        get() = credentialSubject.lastname

    override val dateOfBirth: LocalDate
        get() = credentialSubject.dateOfBirth

    override val portraitRaw: ByteArray?
        get() = credentialSubject.portrait

    override val ageAtLeast14: Boolean?
        get() = credentialSubject.ageOver14

    override val ageAtLeast16: Boolean?
        get() = credentialSubject.ageOver16

    override val ageAtLeast18: Boolean?
        get() = credentialSubject.ageOver18

    override val ageAtLeast21: Boolean?
        get() = credentialSubject.ageOver21

    override val mainAddressRaw: String?
        get() = credentialSubject.mainAddress
}

private class IdAustriaCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
    decodeImage: (ByteArray) -> ImageBitmap?,
) : IdAustriaCredentialAdapter(decodeImage) {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val bpk: String?
        get() = attributes[IdAustriaScheme.Attributes.BPK]?.contentOrNull

    override val givenName: String?
        get() = attributes[IdAustriaScheme.Attributes.FIRSTNAME]?.contentOrNull

    override val familyName: String?
        get() = attributes[IdAustriaScheme.Attributes.LASTNAME]?.contentOrNull

    override val dateOfBirth: LocalDate?
        get() = attributes[IdAustriaScheme.Attributes.DATE_OF_BIRTH]?.contentOrNull?.toLocalDateOrNull()

    override val portraitRaw: ByteArray?
        get() = attributes[IdAustriaScheme.Attributes.PORTRAIT]?.contentOrNull?.decodeToByteArray(Base64UrlStrict)

    override val ageAtLeast14: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_14]?.booleanOrNull

    override val ageAtLeast16: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_16]?.booleanOrNull

    override val ageAtLeast18: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_18]?.booleanOrNull

    override val ageAtLeast21: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_21]?.booleanOrNull

    override val mainAddressRaw: String?
        get() = attributes[IdAustriaScheme.Attributes.MAIN_ADDRESS]?.contentOrNull
}

private class IdAustriaCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    decodeImage: (ByteArray) -> ImageBitmap?,
) : IdAustriaCredentialAdapter(decodeImage) {
    private val idAustriaNamespace = namespaces?.get(IdAustriaScheme.isoNamespace)

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.ISO_MDOC

    override val bpk: String?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.BPK) as String?

    override val givenName: String?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.FIRSTNAME) as String?

    override val familyName: String?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.LASTNAME) as String?

    override val dateOfBirth: LocalDate?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.DATE_OF_BIRTH)?.toLocalDateOrNull()

    override val portraitRaw: ByteArray? by lazy {
        idAustriaNamespace?.get(IdAustriaScheme.Attributes.PORTRAIT)?.let {
            when (it) {
                is ByteArray -> it
                is String -> it.decodeBase64Bytes()
                else -> null
            }
        }
    }

    override val ageAtLeast14: Boolean?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.AGE_OVER_14) as Boolean?

    override val ageAtLeast16: Boolean?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.AGE_OVER_16) as Boolean?

    override val ageAtLeast18: Boolean?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.AGE_OVER_18) as Boolean?

    override val ageAtLeast21: Boolean?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.AGE_OVER_21) as Boolean?

    override val mainAddressRaw: String?
        get() = idAustriaNamespace?.get(IdAustriaScheme.Attributes.MAIN_ADDRESS) as String?
}
