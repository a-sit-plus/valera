package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_door
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_house_number
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_location
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_code
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_name
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_postal_code
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_stair
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_street
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

sealed class IdAustriaCredentialAdapter(
    private val decodePortrait: (ByteArray) -> ImageBitmap,
) : CredentialAdapter {
    override val scheme = IdAustriaScheme
    override fun getAttribute(path: NormalizedJsonPath) =
        path.segments.firstOrNull()?.let { first ->
            when (first) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
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
                        val second = path.segments.getOrNull(1)
                        if (second == null) {
                            Attribute.fromValue(mainAddressRaw)
                        } else when (second) {
                            is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                                IdAustriaCredentialMainAddress.GEMEINDEKENNZIFFER -> Attribute.fromValue(mainAddress?.municipalityCode)
                                IdAustriaCredentialMainAddress.GEMEINDEBEZEICHNUNG -> Attribute.fromValue(mainAddress?.municipalityName)
                                IdAustriaCredentialMainAddress.POSTLEITZAHL -> Attribute.fromValue(mainAddress?.postalCode)
                                IdAustriaCredentialMainAddress.ORTSCHAFT -> Attribute.fromValue(mainAddress?.locality)
                                IdAustriaCredentialMainAddress.STRASSE -> Attribute.fromValue(mainAddress?.street)
                                IdAustriaCredentialMainAddress.HAUSNUMMER -> Attribute.fromValue(mainAddress?.houseNumber)
                                IdAustriaCredentialMainAddress.STIEGE -> Attribute.fromValue(mainAddress?.stair)
                                IdAustriaCredentialMainAddress.TUER -> Attribute.fromValue(mainAddress?.door)
                                else -> null
                            }

                            else -> null
                        }
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
            Json.decodeFromString<IdAustriaCredentialMainAddress>(it.decodeBase64String())
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