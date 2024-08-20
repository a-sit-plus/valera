package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate

sealed class MobileDrivingLicenceCredentialAdapter(
    private val decodePortrait: (ByteArray) -> ImageBitmap,
) : CredentialAdapter {
    override fun getAttribute(path: NormalizedJsonPath) =
        when (val first = path.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                MobileDrivingLicenceScheme.isoNamespace -> getAttributeFromMdlNamespace(
                    NormalizedJsonPath(path.segments.slice(1..path.segments.lastIndex))
                )

                // maybe the attribute is specified without its corresponding namespace - which is definitely the case at the moment!
                else -> getAttributeFromMdlNamespace(path)
            }

            else -> null
        }

    private fun getAttributeFromMdlNamespace(path: NormalizedJsonPath): Attribute? =
        when (val attribute = path.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (attribute.memberName) {
                MobileDrivingLicenceDataElements.FAMILY_NAME -> Attribute.fromValue(familyName)
                MobileDrivingLicenceDataElements.GIVEN_NAME -> Attribute.fromValue(givenName)
                MobileDrivingLicenceDataElements.BIRTH_DATE -> Attribute.fromValue(birthDate)
                MobileDrivingLicenceDataElements.AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                MobileDrivingLicenceDataElements.NATIONALITY -> Attribute.fromValue(nationality)
                MobileDrivingLicenceDataElements.RESIDENT_ADDRESS -> Attribute.fromValue(
                    residentAddress
                )

                MobileDrivingLicenceDataElements.RESIDENT_CITY -> Attribute.fromValue(residentCity)
                MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE -> Attribute.fromValue(
                    residentPostalCode
                )

                MobileDrivingLicenceDataElements.RESIDENT_COUNTRY -> Attribute.fromValue(
                    residentCountry
                )

                MobileDrivingLicenceDataElements.RESIDENT_STATE -> Attribute.fromValue(residentState)
                MobileDrivingLicenceDataElements.AGE_IN_YEARS -> Attribute.fromValue(ageInYears)
                MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR -> Attribute.fromValue(ageBirthYear)
                MobileDrivingLicenceDataElements.BIRTH_PLACE -> Attribute.fromValue(birthPlace)
                MobileDrivingLicenceDataElements.PORTRAIT -> Attribute.fromValue(portraitBitmap)
                MobileDrivingLicenceDataElements.DOCUMENT_NUMBER -> Attribute.fromValue(
                    documentNumber
                )

                MobileDrivingLicenceDataElements.ISSUING_AUTHORITY -> Attribute.fromValue(
                    issuingAuthority
                )

                MobileDrivingLicenceDataElements.ISSUE_DATE -> Attribute.fromValue(issueDate)
                MobileDrivingLicenceDataElements.EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                MobileDrivingLicenceDataElements.ISSUING_COUNTRY -> Attribute.fromValue(
                    issuingCountry
                )

                MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES -> Attribute.fromValue(
                    drivingPrivileges
                )

                MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN -> Attribute.fromValue(
                    undistinguishingSign
                )

                else -> null
            }

            else -> null
        }

    abstract val givenName: String
    abstract val familyName: String
    abstract val birthDate: LocalDate
    abstract val ageAtLeast18: Boolean?
    abstract val nationality: String?
    abstract val residentAddress: String?
    abstract val residentCity: String?
    abstract val residentPostalCode: String?
    abstract val residentCountry: String?
    abstract val residentState: String?
    abstract val ageInYears: String?
    abstract val ageBirthYear: String?
    abstract val birthPlace: String?
    abstract val portraitRaw: ByteArray?
    val portraitBitmap: ImageBitmap? by lazy {
        portraitRaw?.let(decodePortrait)
    }
    abstract val documentNumber: String?
    abstract val issuingAuthority: String?
    abstract val issueDate: LocalDate?
    abstract val expiryDate: LocalDate?
    abstract val issuingCountry: String?
    abstract val drivingPrivileges: List<DrivingPrivilege>?
    abstract val undistinguishingSign: String?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
            decodePortrait: (ByteArray) -> ImageBitmap,
        ): MobileDrivingLicenceCredentialAdapter {
            if (storeEntry.scheme !is MobileDrivingLicenceScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    TODO("Operation not yet supported")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    MobileDrivingLicenceCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                        decodePortrait = decodePortrait,
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    MobileDrivingLicenceCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                        decodePortrait = decodePortrait,
                    )
                }
            }
        }
    }
}

private class MobileDrivingLicenceCredentialSdJwtAdapter(
    attributes: Map<String, Any>,
    decodePortrait: (ByteArray) -> ImageBitmap,
) : MobileDrivingLicenceCredentialAdapter(decodePortrait) {
    private val mobileDrivingLicenceNamespaceProxy = MobileDrivingLicenceCredentialIsoMdocAdapter(
        namespaces = mapOf(MobileDrivingLicenceScheme.isoNamespace to attributes),
        decodePortrait = decodePortrait,
    )

    override val givenName: String
        get() = mobileDrivingLicenceNamespaceProxy.givenName

    override val familyName: String
        get() = mobileDrivingLicenceNamespaceProxy.familyName

    override val birthDate: LocalDate
        get() = mobileDrivingLicenceNamespaceProxy.birthDate

    override val ageAtLeast18: Boolean?
        get() = mobileDrivingLicenceNamespaceProxy.ageAtLeast18

    override val nationality: String?
        get() = mobileDrivingLicenceNamespaceProxy.nationality

    override val residentAddress: String?
        get() = mobileDrivingLicenceNamespaceProxy.residentAddress

    override val residentCity: String?
        get() = mobileDrivingLicenceNamespaceProxy.residentCity

    override val residentPostalCode: String?
        get() = mobileDrivingLicenceNamespaceProxy.residentPostalCode

    override val residentCountry: String?
        get() = mobileDrivingLicenceNamespaceProxy.residentCountry

    override val residentState: String?
        get() = mobileDrivingLicenceNamespaceProxy.residentState

    override val ageInYears: String?
        get() = mobileDrivingLicenceNamespaceProxy.ageInYears

    override val ageBirthYear: String?
        get() = mobileDrivingLicenceNamespaceProxy.ageBirthYear

    override val birthPlace: String?
        get() = mobileDrivingLicenceNamespaceProxy.birthPlace

    override val portraitRaw: ByteArray?
        get() = mobileDrivingLicenceNamespaceProxy.portraitRaw

    override val documentNumber: String?
        get() = mobileDrivingLicenceNamespaceProxy.documentNumber

    override val issuingAuthority: String?
        get() = mobileDrivingLicenceNamespaceProxy.issuingAuthority

    override val issueDate: LocalDate?
        get() = mobileDrivingLicenceNamespaceProxy.issueDate

    override val expiryDate: LocalDate?
        get() = mobileDrivingLicenceNamespaceProxy.expiryDate

    override val issuingCountry: String?
        get() = mobileDrivingLicenceNamespaceProxy.issuingCountry

    override val drivingPrivileges: List<DrivingPrivilege>?
        get() = mobileDrivingLicenceNamespaceProxy.drivingPrivileges

    override val undistinguishingSign: String?
        get() = mobileDrivingLicenceNamespaceProxy.undistinguishingSign
}

private class MobileDrivingLicenceCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    decodePortrait: (ByteArray) -> ImageBitmap,
) : MobileDrivingLicenceCredentialAdapter(decodePortrait) {
    private val mobileDrivingLicenceNamespace =
        namespaces?.get(MobileDrivingLicenceScheme.isoNamespace)
            ?: throw IllegalArgumentException("namespaces") // contains required attributes

    override val givenName: String
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.GIVEN_NAME] as String

    override val familyName: String
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.FAMILY_NAME] as String

    override val birthDate: LocalDate
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.BIRTH_DATE].let {
            LocalDate.parse(it as String)
        }

    override val ageAtLeast18: Boolean?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_OVER_18] as Boolean?

    override val nationality: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.NATIONALITY] as String?

    override val residentAddress: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.RESIDENT_ADDRESS] as String?

    override val residentCity: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.RESIDENT_CITY] as String?

    override val residentPostalCode: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE] as String?

    override val residentCountry: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.RESIDENT_COUNTRY] as String?

    override val residentState: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.RESIDENT_STATE] as String?

    override val ageInYears: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_IN_YEARS] as String?

    override val ageBirthYear: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR] as String?

    override val birthPlace: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.BIRTH_PLACE] as String?

    override val portraitRaw: ByteArray?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.PORTRAIT]?.let {
            (it as String).decodeBase64Bytes()
        }

    override val documentNumber: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.DOCUMENT_NUMBER] as String?

    override val issuingAuthority: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUING_AUTHORITY] as String?

    override val issueDate: LocalDate?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUE_DATE]?.toLocalDateOrNull()

    override val expiryDate: LocalDate?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.EXPIRY_DATE]?.toLocalDateOrNull()

    override val issuingCountry: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUING_COUNTRY] as String?

    override val drivingPrivileges: List<DrivingPrivilege>?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES]?.let {
            it as List<DrivingPrivilege>
        }


    override val undistinguishingSign: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN] as String?
}

private fun Any.toLocalDateOrNull() =
    (this as? LocalDate?) ?: (this as String?)?.let { LocalDate.parse(it) }