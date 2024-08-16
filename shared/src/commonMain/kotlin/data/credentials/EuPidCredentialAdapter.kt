package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import kotlinx.datetime.LocalDate

sealed class EuPidCredentialAdapter : CredentialAdapter {
    override val scheme = EuPidScheme
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when(first.memberName) {
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
    val attributes: Map<String, Any>
) : EuPidCredentialAdapter() {
    override val givenName: String
        get() = attributes[EuPidScheme.Attributes.GIVEN_NAME] as String

    override val familyName: String
        get() = attributes[EuPidScheme.Attributes.FAMILY_NAME] as String

    override val birthDate: LocalDate
        get() = attributes[EuPidScheme.Attributes.BIRTH_DATE] as LocalDate

    override val ageAtLeast18: Boolean?
        get() = attributes[EuPidScheme.Attributes.AGE_OVER_18] as Boolean?

    override val residentAddress: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_ADDRESS] as String?

    override val residentStreet: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_STREET] as String?

    override val residentCity: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_CITY] as String?

    override val residentPostalCode: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_POSTAL_CODE] as String?

    override val residentHouseNumber: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER] as String?

    override val residentCountry: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_COUNTRY] as String?

    override val residentState: String?
        get() = attributes[EuPidScheme.Attributes.RESIDENT_STATE] as String?

    override val gender: IsoIec5218Gender?
        get() = attributes[EuPidScheme.Attributes.GENDER] as IsoIec5218Gender?

    override val nationality: String?
        get() = attributes[EuPidScheme.Attributes.NATIONALITY] as String?

    override val ageInYears: UInt?
        get() = attributes[EuPidScheme.Attributes.AGE_IN_YEARS] as UInt?

    override val ageBirthYear: UInt?
        get() = attributes[EuPidScheme.Attributes.AGE_BIRTH_YEAR] as UInt?

    override val familyNameBirth: String?
        get() = attributes[EuPidScheme.Attributes.FAMILY_NAME_BIRTH] as String?

    override val givenNameBirth: String?
        get() = attributes[EuPidScheme.Attributes.GIVEN_NAME_BIRTH] as String?

    override val birthPlace: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_PLACE] as String?

    override val birthCountry: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_COUNTRY] as String?

    override val birthState: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_STATE] as String?

    override val birthCity: String?
        get() = attributes[EuPidScheme.Attributes.BIRTH_CITY] as String?
}

private class EuPidCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : EuPidCredentialAdapter() {
    private val euPidNamespace = namespaces?.get(EuPidScheme.isoNamespace)
        ?: throw IllegalArgumentException("namespaces") // contains required attributes

    private val euPidNamespaceCredentialProxy = EuPidCredentialSdJwtAdapter(euPidNamespace)

    override val givenName: String
        get() = euPidNamespaceCredentialProxy.givenName

    override val familyName: String
        get() = euPidNamespaceCredentialProxy.familyName

    override val birthDate: LocalDate
        get() = euPidNamespaceCredentialProxy.birthDate

    override val ageAtLeast18: Boolean?
        get() = euPidNamespaceCredentialProxy.ageAtLeast18

    override val residentAddress: String?
        get() = euPidNamespaceCredentialProxy.residentAddress

    override val residentStreet: String?
        get() = euPidNamespaceCredentialProxy.residentStreet

    override val residentCity: String?
        get() = euPidNamespaceCredentialProxy.residentCity

    override val residentPostalCode: String?
        get() = euPidNamespaceCredentialProxy.residentPostalCode

    override val residentHouseNumber: String?
        get() = euPidNamespaceCredentialProxy.residentHouseNumber

    override val residentCountry: String?
        get() = euPidNamespaceCredentialProxy.residentCountry

    override val residentState: String?
        get() = euPidNamespaceCredentialProxy.residentState

    override val gender: IsoIec5218Gender?
        get() = euPidNamespaceCredentialProxy.gender

    override val nationality: String?
        get() = euPidNamespaceCredentialProxy.nationality

    override val ageInYears: UInt?
        get() = euPidNamespaceCredentialProxy.ageInYears

    override val ageBirthYear: UInt?
        get() = euPidNamespaceCredentialProxy.ageBirthYear

    override val familyNameBirth: String?
        get() = euPidNamespaceCredentialProxy.familyNameBirth

    override val givenNameBirth: String?
        get() = euPidNamespaceCredentialProxy.givenNameBirth

    override val birthPlace: String?
        get() = euPidNamespaceCredentialProxy.birthPlace

    override val birthCountry: String?
        get() = euPidNamespaceCredentialProxy.birthCountry

    override val birthState: String?
        get() = euPidNamespaceCredentialProxy.birthState

    override val birthCity: String?
        get() = euPidNamespaceCredentialProxy.birthCity
}