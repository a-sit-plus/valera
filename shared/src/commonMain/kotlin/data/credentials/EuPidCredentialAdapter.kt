package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

sealed class EuPidCredentialAdapter : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                EuPidScheme.Attributes.GIVEN_NAME -> Attribute.fromValue(givenName)
                EuPidScheme.Attributes.FAMILY_NAME -> Attribute.fromValue(familyName)
                EuPidScheme.Attributes.BIRTH_DATE -> Attribute.fromValue(birthDate)
                EuPidScheme.Attributes.AGE_OVER_18 -> Attribute.fromValue(ageAtLeast18)
                EuPidScheme.Attributes.RESIDENT_ADDRESS -> Attribute.fromValue(residentAddress)
                EuPidScheme.Attributes.RESIDENT_STREET -> Attribute.fromValue(residentStreet)
                EuPidScheme.Attributes.RESIDENT_CITY -> Attribute.fromValue(residentCity)
                EuPidScheme.Attributes.RESIDENT_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
                EuPidScheme.Attributes.RESIDENT_COUNTRY -> Attribute.fromValue(residentCountry)
                EuPidScheme.Attributes.RESIDENT_STATE -> Attribute.fromValue(residentState)
                EuPidScheme.Attributes.GENDER -> Attribute.fromValue(gender)
                EuPidScheme.Attributes.NATIONALITY -> Attribute.fromValue(nationality)
                EuPidScheme.Attributes.AGE_IN_YEARS -> Attribute.fromValue(ageInYears)
                EuPidScheme.Attributes.AGE_BIRTH_YEAR -> Attribute.fromValue(ageBirthYear)
                EuPidScheme.Attributes.FAMILY_NAME_BIRTH -> Attribute.fromValue(familyNameBirth)
                EuPidScheme.Attributes.GIVEN_NAME_BIRTH -> Attribute.fromValue(givenNameBirth)
                EuPidScheme.Attributes.BIRTH_PLACE -> Attribute.fromValue(birthPlace)
                EuPidScheme.Attributes.BIRTH_COUNTRY -> Attribute.fromValue(birthCountry)
                EuPidScheme.Attributes.BIRTH_STATE -> Attribute.fromValue(birthState)
                EuPidScheme.Attributes.BIRTH_CITY -> Attribute.fromValue(birthCity)
                EuPidScheme.Attributes.ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                EuPidScheme.Attributes.EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                EuPidScheme.Attributes.ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                EuPidScheme.Attributes.DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                EuPidScheme.Attributes.ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                EuPidScheme.Attributes.ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                EuPidScheme.Attributes.ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                else -> null
            }

            else -> null
        }
    }

    abstract val givenName: String?
    abstract val familyName: String?
    abstract val birthDate: LocalDate?
    abstract val ageAtLeast18: Boolean?
    abstract val residentAddress: String?
    abstract val residentStreet: String?
    abstract val residentCity: String?
    abstract val residentPostalCode: String?
    abstract val residentHouseNumber: String?
    abstract val residentCountry: String?
    abstract val residentState: String?
    abstract val gender: IsoIec5218Gender?
    abstract val nationality: String?
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

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): EuPidCredentialAdapter {
            if (storeEntry.scheme !is EuPidScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    (storeEntry.vc.vc.credentialSubject as? EuPidCredential)?.let {
                        EuPidCredentialVcAdapter(it)
                    } ?: throw IllegalArgumentException("storeEntry")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    EuPidCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    EuPidCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                    )
                }
            }
        }
    }
}

private class EuPidCredentialVcAdapter(
    val credentialSubject: EuPidCredential
) : EuPidCredentialAdapter() {
    override val givenName: String
        get() = credentialSubject.givenName

    override val familyName: String
        get() = credentialSubject.familyName

    override val birthDate: LocalDate
        get() = credentialSubject.birthDate

    override val ageAtLeast18: Boolean?
        get() = credentialSubject.ageOver18

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

    override val gender: IsoIec5218Gender?
        get() = credentialSubject.gender

    override val nationality: String?
        get() = credentialSubject.nationality

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

    override val issuanceDate: Instant?
        get() = credentialSubject.issuanceDate

    override val expiryDate: Instant?
        get() = credentialSubject.expiryDate

    override val issuingAuthority: String?
        get() = credentialSubject.issuingAuthority

    override val documentNumber: String?
        get() = credentialSubject.documentNumber

    override val administrativeNumber: String?
        get() = credentialSubject.administrativeNumber

    override val issuingCountry: String?
        get() = credentialSubject.issuingCountry

    override val issuingJurisdiction: String?
        get() = credentialSubject.issuingJurisdiction
}

private class EuPidCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>
) : EuPidCredentialAdapter() {

    override val givenName: String?
        get() = attributes[EuPidScheme.Attributes.GIVEN_NAME]?.contentOrNull

    override val familyName: String?
        get() = attributes[EuPidScheme.Attributes.FAMILY_NAME]?.contentOrNull

    override val birthDate: LocalDate?
        get() = attributes[EuPidScheme.Attributes.BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val ageAtLeast18: Boolean?
        get() = attributes[EuPidScheme.Attributes.AGE_OVER_18]?.booleanOrNull

    override val residentAddress: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_ADDRESS]?.contentOrNull

    override val residentStreet: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_STREET]?.contentOrNull

    override val residentCity: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_CITY]?.contentOrNull

    override val residentPostalCode: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_POSTAL_CODE]?.contentOrNull

    override val residentHouseNumber: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER]?.contentOrNull

    override val residentCountry: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_COUNTRY]?.contentOrNull

    override val residentState: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_STATE]?.contentOrNull

    override val gender: IsoIec5218Gender?
        get() = attributes[EuPidScheme.Attributes.GENDER]?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code } }

    override val nationality: String?
        get() = attributes[EuPidScheme.Attributes.NATIONALITY]?.contentOrNull

    override val ageInYears: UInt?
        get() = attributes[EuPidScheme.Attributes.AGE_IN_YEARS]?.contentOrNull?.toUIntOrNull()

    override val ageBirthYear: UInt?
        get() = attributes[EuPidScheme.Attributes.AGE_BIRTH_YEAR]?.contentOrNull?.toUIntOrNull()

    override val familyNameBirth: String?
        get() = attributes[EuPidScheme.Attributes.FAMILY_NAME_BIRTH]?.contentOrNull

    override val givenNameBirth: String?
        get() = attributes[EuPidScheme.Attributes.GIVEN_NAME_BIRTH]?.contentOrNull

    override val birthPlace: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_PLACE]?.contentOrNull

    override val birthCountry: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_COUNTRY]?.contentOrNull

    override val birthState: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_STATE]?.contentOrNull

    override val birthCity: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_CITY]?.contentOrNull

    override val issuanceDate: Instant?
        get() = attributes[EuPidScheme.Attributes.ISSUANCE_DATE]?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = attributes[EuPidScheme.Attributes.EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = attributes[EuPidScheme.Attributes.ISSUING_AUTHORITY]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[EuPidScheme.Attributes.DOCUMENT_NUMBER]?.contentOrNull

    override val administrativeNumber: String?
        get() = attributes[EuPidScheme.Attributes.ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val issuingCountry: String?
        get() = attributes[EuPidScheme.Attributes.ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[EuPidScheme.Attributes.ISSUING_JURISDICTION]?.contentOrNull
}

private class EuPidCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : EuPidCredentialAdapter() {
    private val euPidNamespace = namespaces?.get(EuPidScheme.isoNamespace)
        ?: throw IllegalArgumentException("namespaces") // contains required attributes

    override val givenName: String?
        get() = euPidNamespace[EuPidScheme.Attributes.GIVEN_NAME] as String?

    override val familyName: String?
        get() = euPidNamespace[EuPidScheme.Attributes.FAMILY_NAME] as String?

    override val birthDate: LocalDate?
        get() = euPidNamespace[EuPidScheme.Attributes.BIRTH_DATE] as? LocalDate?
            ?: euPidNamespace[EuPidScheme.Attributes.BIRTH_DATE]?.toString()?.toLocalDateOrNull()

    override val ageAtLeast18: Boolean?
        get() = euPidNamespace[EuPidScheme.Attributes.AGE_OVER_18] as? Boolean?

    override val residentAddress: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_ADDRESS] as String?

    override val residentStreet: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_STREET] as String?

    override val residentCity: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_CITY] as String?

    override val residentPostalCode: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_POSTAL_CODE]?.toString()

    override val residentHouseNumber: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER]?.toString()

    override val residentCountry: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_COUNTRY] as String?

    override val residentState: String?
        get() = euPidNamespace[EuPidScheme.Attributes.RESIDENT_STATE] as String?

    override val gender: IsoIec5218Gender?
        get() = euPidNamespace[EuPidScheme.Attributes.GENDER]
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code } }

    override val nationality: String?
        get() = euPidNamespace[EuPidScheme.Attributes.NATIONALITY] as String?

    override val ageInYears: UInt?
        get() = euPidNamespace[EuPidScheme.Attributes.AGE_IN_YEARS] as UInt?

    override val ageBirthYear: UInt?
        get() = euPidNamespace[EuPidScheme.Attributes.AGE_BIRTH_YEAR] as UInt?

    override val familyNameBirth: String?
        get() = euPidNamespace[EuPidScheme.Attributes.FAMILY_NAME_BIRTH] as String?

    override val givenNameBirth: String?
        get() = euPidNamespace[EuPidScheme.Attributes.GIVEN_NAME_BIRTH] as String?

    override val birthPlace: String?
        get() = euPidNamespace[EuPidScheme.Attributes.BIRTH_PLACE] as String?

    override val birthCountry: String?
        get() = euPidNamespace[EuPidScheme.Attributes.BIRTH_COUNTRY] as String?

    override val birthState: String?
        get() = euPidNamespace[EuPidScheme.Attributes.BIRTH_STATE] as String?

    override val birthCity: String?
        get() = euPidNamespace[EuPidScheme.Attributes.BIRTH_CITY] as String?

    override val issuanceDate: Instant?
        get() = euPidNamespace[EuPidScheme.Attributes.ISSUANCE_DATE] as Instant?
            ?: euPidNamespace[EuPidScheme.Attributes.ISSUANCE_DATE]?.toString()?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = euPidNamespace[EuPidScheme.Attributes.EXPIRY_DATE] as Instant?
            ?: euPidNamespace[EuPidScheme.Attributes.EXPIRY_DATE]?.toString()?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = euPidNamespace[EuPidScheme.Attributes.ISSUING_AUTHORITY] as String?

    override val documentNumber: String?
        get() = euPidNamespace[EuPidScheme.Attributes.DOCUMENT_NUMBER] as String?

    override val administrativeNumber: String?
        get() = euPidNamespace[EuPidScheme.Attributes.ADMINISTRATIVE_NUMBER] as String?

    override val issuingCountry: String?
        get() = euPidNamespace[EuPidScheme.Attributes.ISSUING_COUNTRY] as String?

    override val issuingJurisdiction: String?
        get() = euPidNamespace[EuPidScheme.Attributes.ISSUING_JURISDICTION] as String?
}
