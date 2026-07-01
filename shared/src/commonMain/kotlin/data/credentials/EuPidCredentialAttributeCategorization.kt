package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupid.EuPidDataElements as Attributes
import data.PersonalDataCategory

@Suppress("DEPRECATION")
object EuPidCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(Attributes) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                PORTRAIT,
                NATIONALITY,
                SEX,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.BirthData to with(Attributes) {
            listOf(
                GIVEN_NAME_BIRTH,
                FAMILY_NAME_BIRTH,
                PLACE_OF_BIRTH,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.ResidenceData to with(Attributes) {
            listOf(
                RESIDENT_STREET,
                RESIDENT_HOUSE_NUMBER,
                RESIDENT_POSTAL_CODE,
                RESIDENT_CITY,
                RESIDENT_COUNTRY,
                RESIDENT_STATE,
                RESIDENT_ADDRESS,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.Metadata to with(Attributes) {
            listOf(
                DOCUMENT_NUMBER,
                ISSUANCE_DATE,
                EXPIRY_DATE,
                ISSUING_COUNTRY,
                ISSUING_AUTHORITY,
                ISSUING_JURISDICTION,
                PERSONAL_ADMINISTRATIVE_NUMBER,
            ).map { NormalizedJsonPath() + it to null }
        },
    ),

    allAttributes = with(Attributes) {
        listOf(
            FAMILY_NAME, GIVEN_NAME, BIRTH_DATE, FAMILY_NAME_BIRTH, GIVEN_NAME_BIRTH, PLACE_OF_BIRTH,
            RESIDENT_ADDRESS, RESIDENT_COUNTRY, RESIDENT_STATE, RESIDENT_CITY, RESIDENT_POSTAL_CODE,
            RESIDENT_STREET, RESIDENT_HOUSE_NUMBER, SEX, NATIONALITY, ISSUANCE_DATE, EXPIRY_DATE,
            ISSUING_AUTHORITY, DOCUMENT_NUMBER, ISSUING_COUNTRY, ISSUING_JURISDICTION,
            PERSONAL_ADMINISTRATIVE_NUMBER, PORTRAIT, EMAIL_ADDRESS, MOBILE_PHONE_NUMBER, TRUST_ANCHOR,
            LOCATION_STATUS,
        )
    }.map {
        NormalizedJsonPath() + it
    },
)
