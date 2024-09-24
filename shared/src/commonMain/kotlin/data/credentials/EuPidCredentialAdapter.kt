package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import kotlinx.datetime.LocalDate

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
                else -> null
            }

            else -> null
        }
    }

    abstract val givenName: String
    abstract val familyName: String
    abstract val birthDate: LocalDate
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
}

private class EuPidCredentialSdJwtAdapter(
    attributes: Map<String, Any>
) : EuPidCredentialAdapter() {
    private val proxy = EuPidCredentialIsoMdocAdapter(
        namespaces = mapOf(EuPidScheme.isoNamespace to attributes)
    )

    override val givenName: String
        get() = proxy.givenName

    override val familyName: String
        get() = proxy.familyName

    override val birthDate: LocalDate
        get() = proxy.birthDate

    override val ageAtLeast18: Boolean?
        get() = proxy.ageAtLeast18

    override val residentAddress: String?
        get() = proxy.residentAddress

    override val residentStreet: String?
        get() = proxy.residentStreet

    override val residentCity: String?
        get() = proxy.residentCity

    override val residentPostalCode: String?
        get() = proxy.residentPostalCode

    override val residentHouseNumber: String?
        get() = proxy.residentHouseNumber

    override val residentCountry: String?
        get() = proxy.residentCountry

    override val residentState: String?
        get() = proxy.residentState

    override val gender: IsoIec5218Gender?
        get() = proxy.gender

    override val nationality: String?
        get() = proxy.nationality

    override val ageInYears: UInt?
        get() = proxy.ageInYears

    override val ageBirthYear: UInt?
        get() = proxy.ageBirthYear

    override val familyNameBirth: String?
        get() = proxy.familyNameBirth

    override val givenNameBirth: String?
        get() = proxy.givenNameBirth

    override val birthPlace: String?
        get() = proxy.birthPlace

    override val birthCountry: String?
        get() = proxy.birthCountry

    override val birthState: String?
        get() = proxy.birthState

    override val birthCity: String?
        get() = proxy.birthCity
}

private class EuPidCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : EuPidCredentialAdapter() {
    private val euPidNamespace = namespaces?.get(EuPidScheme.isoNamespace)
        ?: throw IllegalArgumentException("namespaces") // contains required attributes

    override val givenName: String
        get() = euPidNamespace[EuPidScheme.Attributes.GIVEN_NAME] as String

    override val familyName: String
        get() = euPidNamespace[EuPidScheme.Attributes.FAMILY_NAME] as String

    override val birthDate: LocalDate
        get() = euPidNamespace[EuPidScheme.Attributes.BIRTH_DATE].toLocalDateOrNull()!!

    override val ageAtLeast18: Boolean?
        get() = euPidNamespace[EuPidScheme.Attributes.AGE_OVER_18] as Boolean?

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
}