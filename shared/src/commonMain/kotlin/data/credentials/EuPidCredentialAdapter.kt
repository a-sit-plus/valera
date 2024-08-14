package data.credentials

import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import kotlinx.datetime.LocalDate

sealed interface EuPidCredentialAdapter : CredentialAdapter {
    val givenName: String
    val familyName: String
    val birthDate: LocalDate
    val ageAtLeast18: Boolean?
    val residentAddress: String?
    val residentStreet: String?
    val residentCity: String?
    val residentPostalCode: String?
    val residentHouseNumber: String?
    val residentCountry: String?
    val residentState: String?
    val gender: IsoIec5218Gender?
    val nationality: String?
    val ageInYears: UInt?
    val ageBirthYear: UInt?
    val familyNameBirth: String?
    val givenNameBirth: String?
    val birthPlace: String?
    val birthCountry: String?
    val birthState: String?
    val birthCity: String?

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
) : EuPidCredentialAdapter {
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
) : EuPidCredentialAdapter {
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
) : EuPidCredentialAdapter {
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