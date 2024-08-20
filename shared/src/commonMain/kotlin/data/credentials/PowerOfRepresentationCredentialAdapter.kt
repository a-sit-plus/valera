package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import kotlinx.datetime.LocalDate

sealed class PowerOfRepresentationCredentialAdapter : CredentialAdapter {
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
                EuPidScheme.Attributes.RESIDENT_POSTAL_CODE -> Attribute.fromValue(
                    residentPostalCode
                )
                EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER -> Attribute.fromValue(
                    residentHouseNumber
                )
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
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): PowerOfRepresentationCredentialAdapter {
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