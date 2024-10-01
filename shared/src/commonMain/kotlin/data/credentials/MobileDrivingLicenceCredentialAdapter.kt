package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.DrivingPrivilege
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
    private val decodePortrait: (ByteArray) -> ImageBitmap,
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
        when (val attribute = path.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (attribute.memberName) {
                MobileDrivingLicenceDataElements.FAMILY_NAME -> Attribute.fromValue(familyName)
                MobileDrivingLicenceDataElements.GIVEN_NAME -> Attribute.fromValue(givenName)
                MobileDrivingLicenceDataElements.BIRTH_DATE -> Attribute.fromValue(birthDate)
                MobileDrivingLicenceDataElements.AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                MobileDrivingLicenceDataElements.NATIONALITY -> Attribute.fromValue(nationality)
                MobileDrivingLicenceDataElements.RESIDENT_ADDRESS -> Attribute.fromValue(residentAddress)
                MobileDrivingLicenceDataElements.RESIDENT_CITY -> Attribute.fromValue(residentCity)
                MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                MobileDrivingLicenceDataElements.RESIDENT_COUNTRY -> Attribute.fromValue(residentCountry)
                MobileDrivingLicenceDataElements.RESIDENT_STATE -> Attribute.fromValue(residentState)
                MobileDrivingLicenceDataElements.AGE_IN_YEARS -> Attribute.fromValue(ageInYears)
                MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR -> Attribute.fromValue(ageBirthYear)
                MobileDrivingLicenceDataElements.BIRTH_PLACE -> Attribute.fromValue(birthPlace)
                MobileDrivingLicenceDataElements.PORTRAIT -> Attribute.fromValue(portraitBitmap)
                MobileDrivingLicenceDataElements.DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                MobileDrivingLicenceDataElements.ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                MobileDrivingLicenceDataElements.ISSUE_DATE -> Attribute.fromValue(issueDate)
                MobileDrivingLicenceDataElements.EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                MobileDrivingLicenceDataElements.ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES -> Attribute.fromValue(drivingPrivileges)
                MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN -> Attribute.fromValue(undistinguishingSign)

                else -> null
            }

            else -> null
        }

    abstract val givenName: String?
    abstract val familyName: String?
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
    abstract val documentNumber: String?
    abstract val issuingAuthority: String?
    abstract val issueDate: LocalDate?
    abstract val expiryDate: LocalDate?
    abstract val issuingCountry: String?
    abstract val drivingPrivileges: Array<DrivingPrivilege>?
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
    private val attributes: Map<String, JsonPrimitive>,
    private val decodePortrait: (ByteArray) -> ImageBitmap,
) : MobileDrivingLicenceCredentialAdapter(decodePortrait) {

    override val givenName: String?
        get() = attributes[MobileDrivingLicenceDataElements.GIVEN_NAME]?.contentOrNull

    override val familyName: String?
        get() = attributes[MobileDrivingLicenceDataElements.FAMILY_NAME]?.contentOrNull

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
        get() = attributes[MobileDrivingLicenceDataElements.PORTRAIT]?.contentOrNull?.decodeToByteArray(
            Base64UrlStrict
        )

    override val documentNumber: String?
        get() = attributes[MobileDrivingLicenceDataElements.DOCUMENT_NUMBER]?.contentOrNull

    override val issuingAuthority: String?
        get() = attributes[MobileDrivingLicenceDataElements.ISSUING_AUTHORITY]?.contentOrNull

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
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.BIRTH_DATE].toLocalDateOrNull()!!

    override val ageAtLeast18: Boolean?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.AGE_OVER_18]?.toString()?.toBooleanStrictOrNull()

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

    @Suppress("UNCHECKED_CAST")
    override val drivingPrivileges: Array<DrivingPrivilege>?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES]
            ?.let { it as Array<DrivingPrivilege> }


    override val undistinguishingSign: String?
        get() = mobileDrivingLicenceNamespace[MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN] as String?
}