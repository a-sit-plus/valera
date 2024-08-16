package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import io.github.aakira.napier.Napier
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

sealed class IdAustriaCredentialAdapter(
    private val decodePortrait: (ByteArray) -> ImageBitmap,
) : CredentialAdapter {
    override val scheme = IdAustriaScheme
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when(first.memberName) {
                IdAustriaScheme.Attributes.BPK -> Attribute.fromValue(bpk)
                IdAustriaScheme.Attributes.FIRSTNAME -> Attribute.fromValue(givenName)
                IdAustriaScheme.Attributes.LASTNAME -> Attribute.fromValue(familyName)
                IdAustriaScheme.Attributes.DATE_OF_BIRTH -> Attribute.fromValue(dateOfBirth)
                IdAustriaScheme.Attributes.PORTRAIT -> Attribute.fromValue(portraitBitmap)
                IdAustriaScheme.Attributes.AGE_OVER_14 -> Attribute.fromValue(ageAtLeast14)
                IdAustriaScheme.Attributes.AGE_OVER_16 -> Attribute.fromValue(ageAtLeast16)
                IdAustriaScheme.Attributes.AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                IdAustriaScheme.Attributes.AGE_OVER_21 -> Attribute.fromValue(ageAtLeast21)
                IdAustriaScheme.Attributes.MAIN_ADDRESS -> {
                    path.segments.getOrNull(1)?.let { second ->
                        when (second) {
                            is NormalizedJsonPathSegment.NameSegment -> when(second.memberName) {
                                "Gemeindekennziffer" -> Attribute.fromValue(mainAddress?.municipalityCode)
                                "Gemeindebezeichnung" -> Attribute.fromValue(mainAddress?.municipalityName)
                                "Postleitzahl" -> Attribute.fromValue(mainAddress?.postalCode)
                                "Ortschaft" -> Attribute.fromValue(mainAddress?.locality)
                                "Strasse" -> Attribute.fromValue(mainAddress?.street)
                                "Hausnummer" -> Attribute.fromValue(mainAddress?.doorNumber)
                                "Stiege" -> Attribute.fromValue(mainAddress?.stair)
                                "Tuer" -> Attribute.fromValue(mainAddress?.door)
                                else -> Attribute.fromValue(mainAddressRaw)
                            }
                            else -> null
                        }
                    } ?: Attribute.fromValue(mainAddressRaw)
                }

                else -> null
            }

            else -> null
        }
    }


    abstract val bpk: String
    abstract val givenName: String
    abstract val familyName: String
    abstract val dateOfBirth: LocalDate
    abstract val portraitRaw: ByteArray?
    val portraitBitmap: ImageBitmap? by lazy {
        portraitRaw?.let(decodePortrait)
    }
    abstract val ageAtLeast14: Boolean?
    abstract val ageAtLeast16: Boolean?
    abstract val ageAtLeast18: Boolean?
    abstract val ageAtLeast21: Boolean?
    val mainAddress by lazy {
        mainAddressRaw?.let {
            Json.decodeFromString<IdAustriaCredentialMainAddress>(it)
        }
    }
    abstract val mainAddressRaw: String?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
            decodeImage: (ByteArray) -> ImageBitmap,
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
    decodeImage: (ByteArray) -> ImageBitmap,
) : IdAustriaCredentialAdapter(decodeImage) {
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
    val attributes: Map<String, Any>,
    decodeImage: (ByteArray) -> ImageBitmap,
) : IdAustriaCredentialAdapter(decodeImage) {
    override val bpk: String
        get() = attributes[IdAustriaScheme.Attributes.BPK] as String

    override val givenName: String
        get() = attributes[IdAustriaScheme.Attributes.FIRSTNAME] as String

    override val familyName: String
        get() = attributes[IdAustriaScheme.Attributes.LASTNAME] as String

    override val dateOfBirth: LocalDate
        get() = attributes[IdAustriaScheme.Attributes.DATE_OF_BIRTH].let {
            LocalDate.parse(it as String)
        }

    override val portraitRaw: ByteArray? by lazy {
        attributes[IdAustriaScheme.Attributes.PORTRAIT]?.let {
            (it as String).decodeBase64Bytes()
        }
    }

    override val ageAtLeast14: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_14] as Boolean?

    override val ageAtLeast16: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_16] as Boolean?

    override val ageAtLeast18: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_18] as Boolean?

    override val ageAtLeast21: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_21] as Boolean?

    override val mainAddressRaw: String?
        get() = attributes[IdAustriaScheme.Attributes.MAIN_ADDRESS] as String?
}

private class IdAustriaCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    decodeImage: (ByteArray) -> ImageBitmap,
) : IdAustriaCredentialAdapter(decodeImage) {
    private val idAustriaNamespace = namespaces?.get(IdAustriaScheme.isoNamespace)
        ?: throw IllegalArgumentException("namespaces") // contains required attributes

    private val idAustriaNamespaceProxy = IdAustriaCredentialSdJwtAdapter(
        idAustriaNamespace,
        decodeImage = decodeImage,
    )

    override val bpk: String
        get() = idAustriaNamespaceProxy.bpk

    override val givenName: String
        get() = idAustriaNamespaceProxy.givenName

    override val familyName: String
        get() = idAustriaNamespaceProxy.familyName

    override val dateOfBirth: LocalDate
        get() = idAustriaNamespaceProxy.dateOfBirth

    override val portraitRaw: ByteArray?
        get() = idAustriaNamespaceProxy.portraitRaw

    override val ageAtLeast14: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast14

    override val ageAtLeast16: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast16

    override val ageAtLeast18: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast18

    override val ageAtLeast21: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast21

    override val mainAddressRaw: String?
        get() = idAustriaNamespaceProxy.mainAddressRaw
}