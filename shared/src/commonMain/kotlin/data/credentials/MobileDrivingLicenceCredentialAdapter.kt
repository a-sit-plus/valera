package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.IsoSexEnum
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.Attribute
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate

sealed class MobileDrivingLicenceCredentialAdapter(
    private val decodePortrait: (ByteArray) -> ImageBitmap?,
) : CredentialAdapter() {
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
        with(MobileDrivingLicenceDataElements) {
            when (val attribute = path.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (attribute.memberName) {
                    GIVEN_NAME -> Attribute.fromValue(givenName)
                    FAMILY_NAME -> Attribute.fromValue(familyName)
                    BIRTH_DATE -> Attribute.fromValue(birthDate)
                    ISSUE_DATE -> Attribute.fromValue(issueDate)
                    EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                    ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                    ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                    DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                    PORTRAIT -> Attribute.fromValue(portraitBitmap)
                    DRIVING_PRIVILEGES -> Attribute.fromValue(drivingPrivileges)
                    UN_DISTINGUISHING_SIGN -> Attribute.fromValue(undistinguishingSign)
                    ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                    SEX -> Attribute.fromValue(sex)
                    HEIGHT -> Attribute.fromValue(height)
                    WEIGHT -> Attribute.fromValue(weight)
                    EYE_COLOUR -> Attribute.fromValue(eyeColour)
                    HAIR_COLOUR -> Attribute.fromValue(hairColour)
                    BIRTH_PLACE -> Attribute.fromValue(birthPlace)
                    RESIDENT_ADDRESS -> Attribute.fromValue(residentAddress)
                    PORTRAIT_CAPTURE_DATE -> Attribute.fromValue(portraitCaptureDate)
                    AGE_IN_YEARS -> Attribute.fromValue(ageInYears)
                    AGE_BIRTH_YEAR -> Attribute.fromValue(ageBirthYear)
                    AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                    ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                    NATIONALITY -> Attribute.fromValue(nationality)
                    RESIDENT_CITY -> Attribute.fromValue(residentCity)
                    RESIDENT_STATE -> Attribute.fromValue(residentState)
                    RESIDENT_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                    RESIDENT_COUNTRY -> Attribute.fromValue(residentCountry)
                    FAMILY_NAME_NATIONAL_CHARACTER -> Attribute.fromValue(familyNameNational)
                    GIVEN_NAME_NATIONAL_CHARACTER -> Attribute.fromValue(givenNameNational)
                    SIGNATURE_USUAL_MARK -> Attribute.fromValue(signatureBitmap)
                    else -> null
                }

                else -> null
            }
        }

    abstract val givenName: String
    abstract val givenNameNational: String?
    abstract val familyName: String
    abstract val familyNameNational: String?
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
    abstract val signatureRaw: ByteArray?
    val signatureBitmap: ImageBitmap? by lazy {
        signatureRaw?.let(decodePortrait)
    }
    abstract val documentNumber: String?
    abstract val administrativeNumber: String?
    abstract val sex: String?
    abstract val height: UInt?
    abstract val weight: UInt?
    abstract val eyeColour: String?
    abstract val hairColour: String?
    abstract val portraitCaptureDate: LocalDate?
    abstract val issuingAuthority: String?
    abstract val issuingJurisdiction: String?
    abstract val issueDate: LocalDate?
    abstract val expiryDate: LocalDate?
    abstract val issuingCountry: String?
    abstract val drivingPrivileges: List<DrivingPrivilege>?
    abstract val undistinguishingSign: String?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
            decodePortrait: (ByteArray) -> ImageBitmap?,
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
    decodePortrait: (ByteArray) -> ImageBitmap?,
) : MobileDrivingLicenceCredentialAdapter(decodePortrait) {
    private val proxy = MobileDrivingLicenceCredentialIsoMdocAdapter(
        namespaces = mapOf(MobileDrivingLicenceScheme.isoNamespace to attributes),
        decodePortrait = decodePortrait,
    )

    override val givenName: String
        get() = proxy.givenName

    override val givenNameNational: String?
        get() = proxy.givenNameNational

    override val familyName: String
        get() = proxy.familyName

    override val familyNameNational: String?
        get() = proxy.familyNameNational

    override val birthDate: LocalDate
        get() = proxy.birthDate

    override val ageAtLeast18: Boolean?
        get() = proxy.ageAtLeast18

    override val nationality: String?
        get() = proxy.nationality

    override val residentAddress: String?
        get() = proxy.residentAddress

    override val residentCity: String?
        get() = proxy.residentCity

    override val residentPostalCode: String?
        get() = proxy.residentPostalCode

    override val residentCountry: String?
        get() = proxy.residentCountry

    override val residentState: String?
        get() = proxy.residentState

    override val ageInYears: String?
        get() = proxy.ageInYears

    override val ageBirthYear: String?
        get() = proxy.ageBirthYear

    override val birthPlace: String?
        get() = proxy.birthPlace

    override val portraitRaw: ByteArray?
        get() = proxy.portraitRaw

    override val signatureRaw: ByteArray?
        get() = proxy.signatureRaw

    override val documentNumber: String?
        get() = proxy.documentNumber

    override val administrativeNumber: String?
        get() = proxy.administrativeNumber

    override val sex: String?
        get() = proxy.sex

    override val height: UInt?
        get() = proxy.height

    override val weight: UInt?
        get() = proxy.weight

    override val eyeColour: String?
        get() = proxy.eyeColour

    override val hairColour: String?
        get() = proxy.issuingAuthority

    override val portraitCaptureDate: LocalDate?
        get() = proxy.portraitCaptureDate

    override val issuingAuthority: String?
        get() = proxy.issuingAuthority

    override val issuingJurisdiction: String?
        get() = proxy.issuingJurisdiction

    override val issueDate: LocalDate?
        get() = proxy.issueDate

    override val expiryDate: LocalDate?
        get() = proxy.expiryDate

    override val issuingCountry: String?
        get() = proxy.issuingCountry

    override val drivingPrivileges: List<DrivingPrivilege>?
        get() = proxy.drivingPrivileges

    override val undistinguishingSign: String?
        get() = proxy.undistinguishingSign
}

private class MobileDrivingLicenceCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    decodePortrait: (ByteArray) -> ImageBitmap?,
) : MobileDrivingLicenceCredentialAdapter(decodePortrait) {
    private val mobileDrivingLicenceNamespace =
        namespaces?.get(MobileDrivingLicenceScheme.isoNamespace)
            ?: throw IllegalArgumentException("namespaces") // contains required attributes

    override val givenName: String
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.GIVEN_NAME] as String

    override val givenNameNational: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.GIVEN_NAME_NATIONAL_CHARACTER] as String?

    override val familyName: String
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.FAMILY_NAME] as String

    override val familyNameNational: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.FAMILY_NAME_NATIONAL_CHARACTER] as String?

    override val birthDate: LocalDate
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.BIRTH_DATE].toLocalDateOrNull()!!

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
            when (it) {
                is ByteArray -> it
                is String -> it.decodeBase64Bytes()
                else -> null
            }
        }

    override val signatureRaw: ByteArray?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.SIGNATURE_USUAL_MARK]?.let {
            when (it) {
                is ByteArray -> it
                is String -> it.decodeBase64Bytes()
                else -> null
            }
        }

    override val documentNumber: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.DOCUMENT_NUMBER] as String?

    override val administrativeNumber: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ADMINISTRATIVE_NUMBER] as String?

    override val sex: String?
        get() = (mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.SEX] as IsoSexEnum?)?.name

    override val height: UInt?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.HEIGHT] as UInt?

    override val weight: UInt?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.WEIGHT] as UInt?

    override val eyeColour: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.EYE_COLOUR] as String?

    override val hairColour: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.HAIR_COLOUR] as String?

    override val portraitCaptureDate: LocalDate?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.PORTRAIT_CAPTURE_DATE]?.toLocalDateOrNull()

    override val issuingAuthority: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUING_AUTHORITY] as String?

    override val issuingJurisdiction: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUING_JURISDICTION] as String?

    override val issueDate: LocalDate?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUE_DATE]?.toLocalDateOrNull()

    override val expiryDate: LocalDate?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.EXPIRY_DATE]?.toLocalDateOrNull()

    override val issuingCountry: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.ISSUING_COUNTRY] as String?

    override val drivingPrivileges: List<DrivingPrivilege>?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES]
            ?.let { it as List<DrivingPrivilege> }

    override val undistinguishingSign: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN] as String?
}