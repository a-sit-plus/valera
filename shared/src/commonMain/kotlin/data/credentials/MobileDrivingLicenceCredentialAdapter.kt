package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.IsoSexEnum
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.Attribute
import io.ktor.util.decodeBase64Bytes
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

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

    abstract val givenName: String?
    abstract val givenNameNational: String?
    abstract val familyName: String?
    abstract val familyNameNational: String?
    abstract val birthDate: LocalDate?
    abstract val ageAtLeast18: Boolean?
    abstract val nationality: String?
    abstract val residentAddress: String?
    abstract val residentCity: String?
    abstract val residentPostalCode: String?
    abstract val residentCountry: String?
    abstract val residentState: String?
    abstract val ageInYears: UInt?
    abstract val ageBirthYear: UInt?
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
    abstract val sex: IsoSexEnum?
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
    abstract val drivingPrivileges: Array<DrivingPrivilege>?
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
    private val attributes: Map<String, JsonPrimitive>,
    private val decodePortrait: (ByteArray) -> ImageBitmap?,
) : MobileDrivingLicenceCredentialAdapter(decodePortrait) {

    override val givenName: String?
        get() = attributes[MobileDrivingLicenceDataElements.GIVEN_NAME]?.contentOrNull

    override val givenNameNational: String?
        get() = attributes[MobileDrivingLicenceDataElements.GIVEN_NAME_NATIONAL_CHARACTER]?.contentOrNull

    override val familyName: String?
        get() = attributes[MobileDrivingLicenceDataElements.FAMILY_NAME]?.contentOrNull

    override val familyNameNational: String?
        get() = attributes[MobileDrivingLicenceDataElements.FAMILY_NAME_NATIONAL_CHARACTER]?.contentOrNull

    override val birthDate: LocalDate?
        get() = attributes[MobileDrivingLicenceDataElements.BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val ageAtLeast18: Boolean?
        get() = attributes[MobileDrivingLicenceDataElements.AGE_OVER_18]?.booleanOrNull

    override val nationality: String?
        get() = attributes[MobileDrivingLicenceDataElements.NATIONALITY]?.contentOrNull

    override val residentAddress: String?
        get() = attributes[MobileDrivingLicenceDataElements.RESIDENT_ADDRESS]?.contentOrNull

    override val residentCity: String?
        get() = attributes[MobileDrivingLicenceDataElements.RESIDENT_CITY]?.contentOrNull

    override val residentPostalCode: String?
        get() = attributes[MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE]?.contentOrNull

    override val residentCountry: String?
        get() = attributes[MobileDrivingLicenceDataElements.RESIDENT_COUNTRY]?.contentOrNull

    override val residentState: String?
        get() = attributes[MobileDrivingLicenceDataElements.RESIDENT_STATE]?.contentOrNull

    override val ageInYears: UInt?
        get() = attributes[MobileDrivingLicenceDataElements.AGE_IN_YEARS]?.contentOrNull?.toUIntOrNull()

    override val ageBirthYear: UInt?
        get() = attributes[MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR]?.contentOrNull?.toUIntOrNull()

    override val birthPlace: String?
        get() = attributes[MobileDrivingLicenceDataElements.BIRTH_PLACE]?.contentOrNull

    override val portraitRaw: ByteArray?
        get() = attributes[MobileDrivingLicenceDataElements.PORTRAIT]?.contentOrNull?.decodeToByteArray(Base64UrlStrict)

    override val signatureRaw: ByteArray?
        get() = attributes[MobileDrivingLicenceDataElements.SIGNATURE_USUAL_MARK]?.contentOrNull
            ?.decodeToByteArray(Base64UrlStrict)

    override val documentNumber: String?
        get() = attributes[MobileDrivingLicenceDataElements.DOCUMENT_NUMBER]?.contentOrNull

    override val administrativeNumber: String?
        get() = attributes[MobileDrivingLicenceDataElements.ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val sex: IsoSexEnum?
        get() = attributes[MobileDrivingLicenceDataElements.SEX]?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoSexEnum.entries.firstOrNull { it.code == code } }

    override val height: UInt?
        get() = attributes[MobileDrivingLicenceDataElements.HEIGHT]?.contentOrNull?.toUIntOrNull()

    override val weight: UInt?
        get() = attributes[MobileDrivingLicenceDataElements.WEIGHT]?.contentOrNull?.toUIntOrNull()

    override val eyeColour: String?
        get() = attributes[MobileDrivingLicenceDataElements.EYE_COLOUR]?.contentOrNull

    override val hairColour: String?
        get() = attributes[MobileDrivingLicenceDataElements.HAIR_COLOUR]?.contentOrNull

    override val portraitCaptureDate: LocalDate?
        get() = attributes[MobileDrivingLicenceDataElements.PORTRAIT_CAPTURE_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val issuingAuthority: String?
        get() = attributes[MobileDrivingLicenceDataElements.ISSUING_AUTHORITY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[MobileDrivingLicenceDataElements.ISSUING_JURISDICTION]?.contentOrNull

    override val issueDate: LocalDate?
        get() = attributes[MobileDrivingLicenceDataElements.ISSUE_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val expiryDate: LocalDate?
        get() = attributes[MobileDrivingLicenceDataElements.EXPIRY_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val issuingCountry: String?
        get() = attributes[MobileDrivingLicenceDataElements.ISSUING_COUNTRY]?.contentOrNull

    // TODO How to decode this?
    override val drivingPrivileges: Array<DrivingPrivilege>?
        get() = arrayOf()

    override val undistinguishingSign: String?
        get() = attributes[MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN]?.contentOrNull
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
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_OVER_18]?.toString()
            ?.toBooleanStrictOrNull()

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

    override val ageInYears: UInt?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_IN_YEARS] as UInt?

    override val ageBirthYear: UInt?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR] as UInt?

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

    override val sex: IsoSexEnum?
        get() = (mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.SEX] as? IsoSexEnum?)
            ?: mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.SEX]?.toString()?.toIntOrNull()
                ?.let { code -> IsoSexEnum.entries.firstOrNull { it.code == code } }

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

    @Suppress("UNCHECKED_CAST")
    override val drivingPrivileges: Array<DrivingPrivilege>?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES]
            ?.let { it as Array<DrivingPrivilege> }


    override val undistinguishingSign: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN] as String?
}
