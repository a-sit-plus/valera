@file:Suppress("DEPRECATION")

package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.eupid.EuPidScheme
import data.PersonalDataCategory

object EuPidCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(EuPidScheme.Attributes) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                PORTRAIT,
                NATIONALITY,
                GENDER,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.AgeData to with(EuPidScheme.Attributes) {
            listOf(
                AGE_OVER_12,
                AGE_OVER_14,
                AGE_OVER_16,
                AGE_OVER_18,
                AGE_OVER_21,
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

object EuPidCredentialSdJwtAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(EuPidScheme.SdJwtAttributes) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                PORTRAIT,
                NATIONALITIES,
                GENDER,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.AgeData to with(EuPidScheme.SdJwtAttributes) {
            listOf(
                AGE_EQUAL_OR_OVER_12,
                AGE_EQUAL_OR_OVER_14,
                AGE_EQUAL_OR_OVER_16,
                AGE_EQUAL_OR_OVER_18,
                AGE_EQUAL_OR_OVER_21,
                AGE_BIRTH_YEAR,
                AGE_IN_YEARS,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.BirthData to with(EuPidScheme.SdJwtAttributes) {
            listOf(
                GIVEN_NAME_BIRTH,
                FAMILY_NAME_BIRTH,
                PLACE_OF_BIRTH_LOCALITY,
                PLACE_OF_BIRTH_COUNTRY,
                PLACE_OF_BIRTH_REGION,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.ResidenceData to with(EuPidScheme.SdJwtAttributes) {
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

        PersonalDataCategory.Metadata to with(EuPidScheme.SdJwtAttributes) {
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
    allAttributes = EuPidScheme.mapIsoToSdJwtAttributes.map {
        NormalizedJsonPath() + it.value
    },
)
