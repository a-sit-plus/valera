package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.eupid.EuPidScheme
import data.PersonalDataCategory

object EuPidCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to listOf(
            EuPidScheme.Attributes.GIVEN_NAME,
            EuPidScheme.Attributes.FAMILY_NAME,
            EuPidScheme.Attributes.BIRTH_DATE,
            EuPidScheme.Attributes.NATIONALITY,
            EuPidScheme.Attributes.GENDER,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.AgeData to listOf(
            EuPidScheme.Attributes.AGE_OVER_18,
            EuPidScheme.Attributes.AGE_BIRTH_YEAR,
            EuPidScheme.Attributes.AGE_IN_YEARS,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.BirthData to listOf(
            EuPidScheme.Attributes.GIVEN_NAME_BIRTH,
            EuPidScheme.Attributes.FAMILY_NAME_BIRTH,
            EuPidScheme.Attributes.BIRTH_PLACE,
            EuPidScheme.Attributes.BIRTH_CITY,
            EuPidScheme.Attributes.BIRTH_COUNTRY,
            EuPidScheme.Attributes.BIRTH_STATE,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.ResidenceData to listOf(
            EuPidScheme.Attributes.RESIDENT_STREET,
            EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER,
            EuPidScheme.Attributes.RESIDENT_POSTAL_CODE,
            EuPidScheme.Attributes.RESIDENT_CITY,
            EuPidScheme.Attributes.RESIDENT_COUNTRY,
            EuPidScheme.Attributes.RESIDENT_STATE,
            EuPidScheme.Attributes.RESIDENT_ADDRESS,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.Metadata to listOf(
            EuPidScheme.Attributes.DOCUMENT_NUMBER,
            EuPidScheme.Attributes.ISSUANCE_DATE,
            EuPidScheme.Attributes.EXPIRY_DATE,
            EuPidScheme.Attributes.ISSUING_COUNTRY,
            EuPidScheme.Attributes.ISSUING_AUTHORITY,
            EuPidScheme.Attributes.ISSUING_JURISDICTION,
            EuPidScheme.Attributes.ADMINISTRATIVE_NUMBER,
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = EuPidScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)