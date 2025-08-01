@file:Suppress("DEPRECATION")

package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import data.PersonalDataCategory

object EuPidCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(EuPidScheme.Attributes) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                PORTRAIT,
                PORTRAIT_CAPTURE_DATE,
                NATIONALITY,
                GENDER,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.AgeData to with(EuPidScheme.Attributes) {
            listOf(
                AGE_OVER_12,
                AGE_OVER_13,
                AGE_OVER_14,
                AGE_OVER_16,
                AGE_OVER_18,
                AGE_OVER_21,
                AGE_OVER_25,
                AGE_OVER_60,
                AGE_OVER_62,
                AGE_OVER_65,
                AGE_OVER_68,
                AGE_BIRTH_YEAR,
                AGE_IN_YEARS,
            ).map { NormalizedJsonPath() + it to null }
        },
        PersonalDataCategory.BirthData to with(EuPidScheme.Attributes) {
            listOf(
                GIVEN_NAME_BIRTH,
                FAMILY_NAME_BIRTH,
                BIRTH_PLACE,
                BIRTH_CITY,
                BIRTH_COUNTRY,
                BIRTH_STATE,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.ResidenceData to with(EuPidScheme.Attributes) {
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

        PersonalDataCategory.Metadata to with(EuPidScheme.Attributes) {
            listOf(
                DOCUMENT_NUMBER,
                ISSUANCE_DATE,
                EXPIRY_DATE,
                ISSUING_COUNTRY,
                ISSUING_AUTHORITY,
                ISSUING_JURISDICTION,
                ADMINISTRATIVE_NUMBER,
                PERSONAL_ADMINISTRATIVE_NUMBER,
            ).map { NormalizedJsonPath() + it to null }
        },
    ),

    allAttributes = EuPidScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)

object EuPidSdJwtCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(EuPidSdJwtScheme.SdJwtAttributes) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                PORTRAIT,
                NATIONALITIES,
                SEX,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.AgeData to with(EuPidSdJwtScheme.SdJwtAttributes) {
            listOf(
                AGE_EQUAL_OR_OVER_12,
                AGE_EQUAL_OR_OVER_13,
                AGE_EQUAL_OR_OVER_14,
                AGE_EQUAL_OR_OVER_16,
                AGE_EQUAL_OR_OVER_18,
                AGE_EQUAL_OR_OVER_21,
                AGE_EQUAL_OR_OVER_25,
                AGE_EQUAL_OR_OVER_60,
                AGE_EQUAL_OR_OVER_62,
                AGE_EQUAL_OR_OVER_65,
                AGE_EQUAL_OR_OVER_68,
                AGE_BIRTH_YEAR,
                AGE_IN_YEARS,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.BirthData to with(EuPidSdJwtScheme.SdJwtAttributes) {
            listOf(
                GIVEN_NAME_BIRTH,
                FAMILY_NAME_BIRTH,
                PLACE_OF_BIRTH_LOCALITY,
                PLACE_OF_BIRTH_COUNTRY,
                PLACE_OF_BIRTH_REGION,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.ResidenceData to with(EuPidSdJwtScheme.SdJwtAttributes) {
            listOf(
                ADDRESS_STREET,
                ADDRESS_HOUSE_NUMBER,
                ADDRESS_POSTAL_CODE,
                ADDRESS_LOCALITY,
                ADDRESS_COUNTRY,
                ADDRESS_REGION,
                ADDRESS_FORMATTED,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.Metadata to with(EuPidSdJwtScheme.SdJwtAttributes) {
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
    allAttributes = EuPidSdJwtScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)
