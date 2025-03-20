@file:Suppress("DEPRECATION")

package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.EuPidScheme.Attributes
import at.asitplus.wallet.eupid.EuPidScheme.SdJwtAttributes
import at.asitplus.wallet.eupid.EuPidScheme.SdJwtAttributes.Address
import at.asitplus.wallet.eupid.EuPidScheme.SdJwtAttributes.AgeEqualOrOver
import at.asitplus.wallet.eupid.EuPidScheme.SdJwtAttributes.PlaceOfBirth
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute
import io.ktor.util.decodeBase64Bytes
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

sealed class EuPidCredentialAdapter(
    private val decodePortrait: (ByteArray) -> ImageBitmap?,
) : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        getWithIsoNames(first) ?: getWithSdJwtNames(first, path.segments.getOrNull(1))
    }

    private fun getWithIsoNames(first: NormalizedJsonPathSegment) = with(Attributes) {
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                GIVEN_NAME -> Attribute.fromValue(givenName)
                FAMILY_NAME -> Attribute.fromValue(familyName)
                BIRTH_DATE -> Attribute.fromValue(birthDate)
                AGE_OVER_12 -> Attribute.fromValue(ageAtLeast12)
                AGE_OVER_14 -> Attribute.fromValue(ageAtLeast14)
                AGE_OVER_16 -> Attribute.fromValue(ageAtLeast16)
                AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                AGE_OVER_21 -> Attribute.fromValue(ageAtLeast21)
                RESIDENT_ADDRESS -> Attribute.fromValue(residentAddress)
                RESIDENT_STREET -> Attribute.fromValue(residentStreet)
                RESIDENT_CITY -> Attribute.fromValue(residentCity)
                RESIDENT_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                RESIDENT_HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
                RESIDENT_COUNTRY -> Attribute.fromValue(residentCountry)
                RESIDENT_STATE -> Attribute.fromValue(residentState)
                GENDER -> Attribute.fromValue(gender)
                SEX -> Attribute.fromValue(sex)
                NATIONALITY -> Attribute.fromValue(nationality)
                AGE_IN_YEARS -> Attribute.fromValue(ageInYears)
                AGE_BIRTH_YEAR -> Attribute.fromValue(ageBirthYear)
                FAMILY_NAME_BIRTH -> Attribute.fromValue(familyNameBirth)
                GIVEN_NAME_BIRTH -> Attribute.fromValue(givenNameBirth)
                BIRTH_PLACE -> Attribute.fromValue(birthPlace)
                BIRTH_COUNTRY -> Attribute.fromValue(birthCountry)
                BIRTH_STATE -> Attribute.fromValue(birthState)
                BIRTH_CITY -> Attribute.fromValue(birthCity)
                ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                PERSONAL_ADMINISTRATIVE_NUMBER -> Attribute.fromValue(personalAdministrativeNumber)
                PORTRAIT -> Attribute.fromValue(portraitBitmap)
                EMAIL_ADDRESS -> Attribute.fromValue(emailAddress)
                MOBILE_PHONE_NUMBER -> Attribute.fromValue(mobilePhoneNumber)
                TRUST_ANCHOR -> Attribute.fromValue(trustAnchor)
                LOCATION_STATUS -> Attribute.fromValue(locationStatus)
                else -> null
            }

            else -> null
        }
    }

    private fun getWithSdJwtNames(
        first: NormalizedJsonPathSegment,
        second: NormalizedJsonPathSegment?
    ) = with(SdJwtAttributes) {
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                GIVEN_NAME -> Attribute.fromValue(givenName)
                FAMILY_NAME -> Attribute.fromValue(familyName)
                BIRTH_DATE -> Attribute.fromValue(birthDate)
                PREFIX_AGE_EQUAL_OR_OVER -> when (second) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        AgeEqualOrOver.EQUAL_OR_OVER_12 -> Attribute.fromValue(ageAtLeast12)
                        AgeEqualOrOver.EQUAL_OR_OVER_14 -> Attribute.fromValue(ageAtLeast14)
                        AgeEqualOrOver.EQUAL_OR_OVER_16 -> Attribute.fromValue(ageAtLeast16)
                        AgeEqualOrOver.EQUAL_OR_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                        AgeEqualOrOver.EQUAL_OR_OVER_21 -> Attribute.fromValue(ageAtLeast21)
                        else -> null
                    }

                    else -> null
                }

                AGE_EQUAL_OR_OVER_12 -> Attribute.fromValue(ageAtLeast12)
                AGE_EQUAL_OR_OVER_14 -> Attribute.fromValue(ageAtLeast14)
                AGE_EQUAL_OR_OVER_16 -> Attribute.fromValue(ageAtLeast16)
                AGE_EQUAL_OR_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                AGE_EQUAL_OR_OVER_21 -> Attribute.fromValue(ageAtLeast21)
                PREFIX_ADDRESS -> when (second) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        Address.FORMATTED -> Attribute.fromValue(residentAddress)
                        Address.STREET -> Attribute.fromValue(residentStreet)
                        Address.LOCALITY -> Attribute.fromValue(residentCity)
                        Address.POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                        Address.HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
                        Address.COUNTRY -> Attribute.fromValue(residentCountry)
                        Address.REGION -> Attribute.fromValue(residentState)
                        else -> null
                    }

                    else -> null
                }

                ADDRESS_FORMATTED -> Attribute.fromValue(residentAddress)
                ADDRESS_STREET -> Attribute.fromValue(residentStreet)
                ADDRESS_LOCALITY -> Attribute.fromValue(residentCity)
                ADDRESS_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                ADDRESS_HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
                ADDRESS_COUNTRY -> Attribute.fromValue(residentCountry)
                ADDRESS_REGION -> Attribute.fromValue(residentState)
                GENDER -> Attribute.fromValue(gender)
                NATIONALITIES -> Attribute.fromValue(nationalities)
                AGE_IN_YEARS -> Attribute.fromValue(ageInYears)
                AGE_BIRTH_YEAR -> Attribute.fromValue(ageBirthYear)
                FAMILY_NAME_BIRTH -> Attribute.fromValue(familyNameBirth)
                GIVEN_NAME_BIRTH -> Attribute.fromValue(givenNameBirth)
                PREFIX_PLACE_OF_BIRTH -> when (second) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        PlaceOfBirth.COUNTRY -> Attribute.fromValue(birthCountry)
                        PlaceOfBirth.REGION -> Attribute.fromValue(birthState)
                        PlaceOfBirth.LOCALITY -> Attribute.fromValue(birthCity)
                        else -> null
                    }

                    else -> null
                }

                PLACE_OF_BIRTH_COUNTRY -> Attribute.fromValue(birthCountry)
                PLACE_OF_BIRTH_REGION -> Attribute.fromValue(birthState)
                PLACE_OF_BIRTH_LOCALITY -> Attribute.fromValue(birthCity)
                ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                PERSONAL_ADMINISTRATIVE_NUMBER -> Attribute.fromValue(personalAdministrativeNumber)
                PORTRAIT -> Attribute.fromValue(portraitBitmap)
                EMAIL -> Attribute.fromValue(emailAddress)
                PHONE_NUMBER -> Attribute.fromValue(mobilePhoneNumber)
                TRUST_ANCHOR -> Attribute.fromValue(trustAnchor)
                LOCATION_STATUS -> Attribute.fromValue(locationStatus)
                else -> null
            }

            else -> null
        }
    }

    abstract val givenName: String?
    abstract val familyName: String?
    abstract val birthDate: LocalDate?
    abstract val portraitRaw: ByteArray?
    val portraitBitmap: ImageBitmap? by lazy {
        portraitRaw?.let(decodePortrait)
    }
    abstract val ageAtLeast12: Boolean?
    abstract val ageAtLeast14: Boolean?
    abstract val ageAtLeast16: Boolean?
    abstract val ageAtLeast18: Boolean?
    abstract val ageAtLeast21: Boolean?
    abstract val residentAddress: String?
    abstract val residentStreet: String?
    abstract val residentCity: String?
    abstract val residentPostalCode: String?
    abstract val residentHouseNumber: String?
    abstract val residentCountry: String?
    abstract val residentState: String?
    abstract val gender: String?
    abstract val sex: String?
    abstract val nationality: String?
    abstract val nationalities: Collection<String>?
    abstract val ageInYears: UInt?
    abstract val ageBirthYear: UInt?
    abstract val familyNameBirth: String?
    abstract val givenNameBirth: String?
    abstract val birthPlace: String?
    abstract val birthCountry: String?
    abstract val birthState: String?
    abstract val birthCity: String?
    abstract val issuanceDate: Instant?
    abstract val expiryDate: Instant?
    abstract val issuingAuthority: String?
    abstract val documentNumber: String?
    abstract val administrativeNumber: String?
    abstract val issuingCountry: String?
    abstract val issuingJurisdiction: String?
    abstract val personalAdministrativeNumber: String?
    abstract val emailAddress: String?
    abstract val mobilePhoneNumber: String?
    abstract val trustAnchor: String?
    abstract val locationStatus: String?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
            decodePortrait: (ByteArray) -> ImageBitmap?,
        ): EuPidCredentialAdapter {
            if (storeEntry.scheme !is EuPidScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc ->
                    (storeEntry.vc.vc.credentialSubject as? EuPidCredential)
                        ?.let { EuPidCredentialVcAdapter(it, decodePortrait) }
                        ?: throw IllegalArgumentException("storeEntry")

                is SubjectCredentialStore.StoreEntry.SdJwt ->
                    EuPidCredentialSdJwtAdapter(storeEntry.toAttributeMap(), decodePortrait)

                is SubjectCredentialStore.StoreEntry.Iso ->
                    EuPidCredentialIsoMdocAdapter(storeEntry.toNamespaceAttributeMap(), decodePortrait)
            }
        }
    }
}

private class EuPidCredentialVcAdapter(
    val credentialSubject: EuPidCredential,
    decodePortrait: (ByteArray) -> ImageBitmap?,
) : EuPidCredentialAdapter(decodePortrait) {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.PLAIN_JWT

    override val givenName: String
        get() = credentialSubject.givenName

    override val familyName: String
        get() = credentialSubject.familyName

    override val birthDate: LocalDate
        get() = credentialSubject.birthDate

    override val portraitRaw: ByteArray?
        get() = credentialSubject.portrait

    override val ageAtLeast12: Boolean?
        get() = credentialSubject.ageOver12

    override val ageAtLeast14: Boolean?
        get() = credentialSubject.ageOver14

    override val ageAtLeast16: Boolean?
        get() = credentialSubject.ageOver16

    override val ageAtLeast18: Boolean?
        get() = credentialSubject.ageOver18

    override val ageAtLeast21: Boolean?
        get() = credentialSubject.ageOver21

    override val residentAddress: String?
        get() = credentialSubject.residentAddress

    override val residentStreet: String?
        get() = credentialSubject.residentStreet

    override val residentCity: String?
        get() = credentialSubject.residentCity

    override val residentPostalCode: String?
        get() = credentialSubject.residentPostalCode

    override val residentHouseNumber: String?
        get() = credentialSubject.residentHouseNumber

    override val residentCountry: String?
        get() = credentialSubject.residentCountry

    override val residentState: String?
        get() = credentialSubject.residentState

    override val gender: String?
        get() = credentialSubject.gender?.name

    override val sex: String?
        get() = credentialSubject.sexAsEnum?.name

    override val nationality: String?
        get() = credentialSubject.nationality

    override val nationalities: Collection<String>?
        get() = credentialSubject.nationalities
            ?: listOfNotNull(credentialSubject.nationality).ifEmpty { null }

    override val ageInYears: UInt?
        get() = credentialSubject.ageInYears

    override val ageBirthYear: UInt?
        get() = credentialSubject.ageBirthYear

    override val familyNameBirth: String?
        get() = credentialSubject.familyNameBirth

    override val givenNameBirth: String?
        get() = credentialSubject.givenNameBirth

    override val birthPlace: String?
        get() = credentialSubject.birthPlace

    override val birthCountry: String?
        get() = credentialSubject.birthCountry

    override val birthState: String?
        get() = credentialSubject.birthState

    override val birthCity: String?
        get() = credentialSubject.birthCity

    override val issuanceDate: Instant
        get() = credentialSubject.issuanceDate

    override val expiryDate: Instant
        get() = credentialSubject.expiryDate

    override val issuingAuthority: String
        get() = credentialSubject.issuingAuthority

    override val documentNumber: String?
        get() = credentialSubject.documentNumber

    override val administrativeNumber: String?
        get() = credentialSubject.administrativeNumber

    override val issuingCountry: String
        get() = credentialSubject.issuingCountry

    override val issuingJurisdiction: String?
        get() = credentialSubject.issuingJurisdiction

    override val personalAdministrativeNumber: String?
        get() = credentialSubject.personalAdministrativeNumber

    override val emailAddress: String?
        get() = credentialSubject.emailAddress

    override val mobilePhoneNumber: String?
        get() = credentialSubject.mobilePhoneNumber

    override val trustAnchor: String?
        get() = credentialSubject.trustAnchor

    override val locationStatus: String?
        get() = credentialSubject.locationStatus
}

/**
 * Implements getting attributes for new names (from [SdJwtAttributes]),
 * as well as for old names (from [Attributes.GIVEN_NAME]), to keep data for credentials loaded before migration
 */
private class EuPidCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
    decodePortrait: (ByteArray) -> ImageBitmap?,
) : EuPidCredentialAdapter(decodePortrait) {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val givenName: String?
        get() = attributes[SdJwtAttributes.GIVEN_NAME]?.contentOrNull
            ?: attributes[Attributes.GIVEN_NAME]?.contentOrNull

    override val familyName: String?
        get() = attributes[SdJwtAttributes.FAMILY_NAME]?.contentOrNull
            ?: attributes[Attributes.FAMILY_NAME]?.contentOrNull

    override val birthDate: LocalDate?
        get() = attributes[SdJwtAttributes.BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()
            ?: attributes[Attributes.BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val portraitRaw: ByteArray?
        get() = attributes[SdJwtAttributes.PORTRAIT]?.contentOrNull?.decodeToByteArray(Base64UrlStrict)
            ?: attributes[Attributes.PORTRAIT]?.contentOrNull?.decodeToByteArray(Base64UrlStrict)

    override val ageAtLeast12: Boolean?
        get() = attributes[SdJwtAttributes.AGE_EQUAL_OR_OVER_12]?.booleanOrNull
            ?: attributes[Attributes.AGE_OVER_12]?.booleanOrNull

    override val ageAtLeast14: Boolean?
        get() = attributes[SdJwtAttributes.AGE_EQUAL_OR_OVER_14]?.booleanOrNull
            ?: attributes[Attributes.AGE_OVER_14]?.booleanOrNull

    override val ageAtLeast16: Boolean?
        get() = attributes[SdJwtAttributes.AGE_EQUAL_OR_OVER_16]?.booleanOrNull
            ?: attributes[Attributes.AGE_OVER_16]?.booleanOrNull

    override val ageAtLeast18: Boolean?
        get() = attributes[SdJwtAttributes.AGE_EQUAL_OR_OVER_18]?.booleanOrNull
            ?: attributes[Attributes.AGE_OVER_18]?.booleanOrNull

    override val ageAtLeast21: Boolean?
        get() = attributes[SdJwtAttributes.AGE_EQUAL_OR_OVER_21]?.booleanOrNull
            ?: attributes[Attributes.AGE_OVER_21]?.booleanOrNull

    override val residentAddress: String?
        get() = attributes[SdJwtAttributes.ADDRESS_FORMATTED]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_ADDRESS]?.contentOrNull

    override val residentStreet: String?
        get() = attributes[SdJwtAttributes.ADDRESS_STREET]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_STREET]?.contentOrNull

    override val residentCity: String?
        get() = attributes[SdJwtAttributes.ADDRESS_LOCALITY]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_CITY]?.contentOrNull

    override val residentPostalCode: String?
        get() = attributes[SdJwtAttributes.ADDRESS_POSTAL_CODE]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_POSTAL_CODE]?.contentOrNull

    override val residentHouseNumber: String?
        get() = attributes[SdJwtAttributes.ADDRESS_HOUSE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_HOUSE_NUMBER]?.contentOrNull

    override val residentCountry: String?
        get() = attributes[SdJwtAttributes.ADDRESS_COUNTRY]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_COUNTRY]?.contentOrNull

    override val residentState: String?
        get() = attributes[SdJwtAttributes.ADDRESS_REGION]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_STATE]?.contentOrNull

    override val gender: String?
        get() = attributes[Attributes.GENDER]?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code.toUInt() }?.name }
            ?: attributes[SdJwtAttributes.GENDER]?.contentOrNull
            ?: attributes[Attributes.GENDER]?.contentOrNull

    override val sex: String?
        get() = attributes[Attributes.SEX]?.contentOrNull?.toUIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code }?.name }

    override val nationality: String?
        get() = attributes[Attributes.NATIONALITY]?.contentOrNull

    override val nationalities: Collection<String>?
        get() = attributes[SdJwtAttributes.NATIONALITIES]?.toCollectionOrNull()
            ?: listOfNotNull(nationality).ifEmpty { null }

    override val ageInYears: UInt?
        get() = attributes[SdJwtAttributes.AGE_IN_YEARS]?.contentOrNull?.toUIntOrNull()
            ?: attributes[Attributes.AGE_IN_YEARS]?.contentOrNull?.toUIntOrNull()

    override val ageBirthYear: UInt?
        get() = attributes[SdJwtAttributes.AGE_BIRTH_YEAR]?.contentOrNull?.toUIntOrNull()
            ?: attributes[Attributes.AGE_BIRTH_YEAR]?.contentOrNull?.toUIntOrNull()

    override val familyNameBirth: String?
        get() = attributes[SdJwtAttributes.FAMILY_NAME_BIRTH]?.contentOrNull
            ?: attributes[Attributes.FAMILY_NAME_BIRTH]?.contentOrNull

    override val givenNameBirth: String?
        get() = attributes[SdJwtAttributes.GIVEN_NAME_BIRTH]?.contentOrNull
            ?: attributes[Attributes.GIVEN_NAME_BIRTH]?.contentOrNull

    override val birthPlace: String?
        get() = attributes[SdJwtAttributes.PLACE_OF_BIRTH_LOCALITY]?.contentOrNull
            ?: attributes[Attributes.BIRTH_PLACE]?.contentOrNull

    override val birthCountry: String?
        get() = attributes[SdJwtAttributes.PLACE_OF_BIRTH_COUNTRY]?.contentOrNull
            ?: attributes[Attributes.BIRTH_COUNTRY]?.contentOrNull

    override val birthState: String?
        get() = attributes[SdJwtAttributes.PLACE_OF_BIRTH_REGION]?.contentOrNull
            ?: attributes[Attributes.BIRTH_STATE]?.contentOrNull

    override val birthCity: String?
        get() = attributes[SdJwtAttributes.PLACE_OF_BIRTH_LOCALITY]?.contentOrNull
            ?: attributes[Attributes.BIRTH_CITY]?.contentOrNull

    override val issuanceDate: Instant?
        get() = attributes[SdJwtAttributes.ISSUANCE_DATE]?.contentOrNull?.toInstantOrNull()
            ?: attributes[Attributes.ISSUANCE_DATE]?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = attributes[SdJwtAttributes.EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()
            ?: attributes[Attributes.EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = attributes[SdJwtAttributes.ISSUING_AUTHORITY]?.contentOrNull
            ?: attributes[Attributes.ISSUING_AUTHORITY]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[SdJwtAttributes.DOCUMENT_NUMBER]?.contentOrNull
            ?: attributes[Attributes.DOCUMENT_NUMBER]?.contentOrNull

    override val administrativeNumber: String?
        get() = attributes[SdJwtAttributes.ADMINISTRATIVE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val issuingCountry: String?
        get() = attributes[SdJwtAttributes.ISSUING_COUNTRY]?.contentOrNull
            ?: attributes[Attributes.ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[SdJwtAttributes.ISSUING_JURISDICTION]?.contentOrNull
            ?: attributes[Attributes.ISSUING_JURISDICTION]?.contentOrNull

    override val personalAdministrativeNumber: String?
        get() = attributes[SdJwtAttributes.PERSONAL_ADMINISTRATIVE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.PERSONAL_ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val emailAddress: String?
        get() = attributes[SdJwtAttributes.EMAIL]?.contentOrNull
            ?: attributes[Attributes.EMAIL_ADDRESS]?.contentOrNull

    override val mobilePhoneNumber: String?
        get() = attributes[SdJwtAttributes.PHONE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.MOBILE_PHONE_NUMBER]?.contentOrNull

    override val trustAnchor: String?
        get() = attributes[SdJwtAttributes.TRUST_ANCHOR]?.contentOrNull
            ?: attributes[Attributes.TRUST_ANCHOR]?.contentOrNull

    override val locationStatus: String?
        get() = attributes[SdJwtAttributes.LOCATION_STATUS]?.contentOrNull
            ?: attributes[Attributes.LOCATION_STATUS]?.contentOrNull
}

private class EuPidCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    decodePortrait: (ByteArray) -> ImageBitmap?,
) : EuPidCredentialAdapter(decodePortrait) {
    private val euPidNamespace = namespaces?.get(EuPidScheme.isoNamespace)

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.ISO_MDOC

    override val givenName: String?
        get() = euPidNamespace?.get(Attributes.GIVEN_NAME) as? String?

    override val familyName: String?
        get() = euPidNamespace?.get(Attributes.FAMILY_NAME) as? String?

    override val birthDate: LocalDate?
        get() = euPidNamespace?.get(Attributes.BIRTH_DATE) as? LocalDate?
            ?: euPidNamespace?.get(Attributes.BIRTH_DATE)?.toString()?.toLocalDateOrNull()

    override val portraitRaw: ByteArray?
        get() = euPidNamespace?.get(Attributes.PORTRAIT)?.let {
            when (it) {
                is ByteArray -> it
                is String -> it.decodeBase64Bytes()
                else -> null
            }
        }

    override val ageAtLeast12: Boolean?
        get() = euPidNamespace?.get(Attributes.AGE_OVER_12) as? Boolean?

    override val ageAtLeast14: Boolean?
        get() = euPidNamespace?.get(Attributes.AGE_OVER_14) as? Boolean?

    override val ageAtLeast16: Boolean?
        get() = euPidNamespace?.get(Attributes.AGE_OVER_16) as? Boolean?

    override val ageAtLeast18: Boolean?
        get() = euPidNamespace?.get(Attributes.AGE_OVER_18) as? Boolean?

    override val ageAtLeast21: Boolean?
        get() = euPidNamespace?.get(Attributes.AGE_OVER_21) as? Boolean?

    override val residentAddress: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_ADDRESS) as? String?

    override val residentStreet: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_STREET) as? String?

    override val residentCity: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_CITY) as? String?

    override val residentPostalCode: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_POSTAL_CODE)?.toString()

    override val residentHouseNumber: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_HOUSE_NUMBER)?.toString()

    override val residentCountry: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_COUNTRY) as? String?

    override val residentState: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_STATE) as? String?

    override val gender: String?
        get() = (euPidNamespace?.get(Attributes.GENDER) as? IsoIec5218Gender)?.name
            ?: (euPidNamespace?.get(Attributes.GENDER) as? Int)
                ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code.toUInt() }?.name }
            ?: (euPidNamespace?.get(Attributes.GENDER) as? UInt)
                ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code }?.name }
            ?: euPidNamespace?.get(Attributes.GENDER) as? String?

    override val sex: String?
        get() = (euPidNamespace?.get(Attributes.SEX) as? UInt)
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code }?.name }

    override val nationality: String?
        get() = euPidNamespace?.get(Attributes.NATIONALITY) as? String?

    override val nationalities: Collection<String>?
        get() = (euPidNamespace?.get(Attributes.NATIONALITY) as? Collection<*>?)?.map { it.toString() }
            ?: listOfNotNull(nationality).ifEmpty { null }

    override val ageInYears: UInt?
        get() = euPidNamespace?.get(Attributes.AGE_IN_YEARS) as UInt?

    override val ageBirthYear: UInt?
        get() = euPidNamespace?.get(Attributes.AGE_BIRTH_YEAR) as UInt?

    override val familyNameBirth: String?
        get() = euPidNamespace?.get(Attributes.FAMILY_NAME_BIRTH) as? String?

    override val givenNameBirth: String?
        get() = euPidNamespace?.get(Attributes.GIVEN_NAME_BIRTH) as? String?

    override val birthPlace: String?
        get() = euPidNamespace?.get(Attributes.BIRTH_PLACE) as? String?

    override val birthCountry: String?
        get() = euPidNamespace?.get(Attributes.BIRTH_COUNTRY) as? String?

    override val birthState: String?
        get() = euPidNamespace?.get(Attributes.BIRTH_STATE) as? String?

    override val birthCity: String?
        get() = euPidNamespace?.get(Attributes.BIRTH_CITY) as? String?

    override val issuanceDate: Instant?
        get() = euPidNamespace?.get(Attributes.ISSUANCE_DATE) as? Instant?
            ?: euPidNamespace?.get(Attributes.ISSUANCE_DATE)?.toString()?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = euPidNamespace?.get(Attributes.EXPIRY_DATE) as? Instant?
            ?: euPidNamespace?.get(Attributes.EXPIRY_DATE)?.toString()?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = euPidNamespace?.get(Attributes.ISSUING_AUTHORITY) as? String?

    override val documentNumber: String?
        get() = euPidNamespace?.get(Attributes.DOCUMENT_NUMBER) as? String?

    override val administrativeNumber: String?
        get() = euPidNamespace?.get(Attributes.ADMINISTRATIVE_NUMBER) as? String?

    override val issuingCountry: String?
        get() = euPidNamespace?.get(Attributes.ISSUING_COUNTRY) as? String?

    override val issuingJurisdiction: String?
        get() = euPidNamespace?.get(Attributes.ISSUING_JURISDICTION) as? String?

    override val personalAdministrativeNumber: String?
        get() = euPidNamespace?.get(Attributes.PERSONAL_ADMINISTRATIVE_NUMBER) as? String?

    override val emailAddress: String?
        get() = euPidNamespace?.get(Attributes.EMAIL_ADDRESS) as? String?

    override val mobilePhoneNumber: String?
        get() = euPidNamespace?.get(Attributes.MOBILE_PHONE_NUMBER) as? String?

    override val trustAnchor: String?
        get() = euPidNamespace?.get(Attributes.TRUST_ANCHOR) as? String?

    override val locationStatus: String?
        get() = euPidNamespace?.get(Attributes.LOCATION_STATUS) as? String?
}
