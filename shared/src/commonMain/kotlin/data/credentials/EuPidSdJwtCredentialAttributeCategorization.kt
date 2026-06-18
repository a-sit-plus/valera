package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements as SdJwtAttributes
import data.PersonalDataCategory

object EuPidSdJwtCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(SdJwtAttributes) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                PORTRAIT,
                NATIONALITIES,
                SEX,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.BirthData to with(SdJwtAttributes) {
            listOf(
                GIVEN_NAME_BIRTH,
                FAMILY_NAME_BIRTH,
                PLACE_OF_BIRTH_LOCALITY,
                PLACE_OF_BIRTH_COUNTRY,
                PLACE_OF_BIRTH_REGION,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.ResidenceData to with(SdJwtAttributes) {
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

        PersonalDataCategory.Metadata to with(SdJwtAttributes) {
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
        // AgeData category dropped: EU PID SD-JWT (vck core) no longer models age_equal_or_over claims.
    ),
    allAttributes = with(SdJwtAttributes) {
        listOf(
            FAMILY_NAME, GIVEN_NAME, BIRTH_DATE, FAMILY_NAME_BIRTH, GIVEN_NAME_BIRTH,
            PLACE_OF_BIRTH_COUNTRY, PLACE_OF_BIRTH_REGION, PLACE_OF_BIRTH_LOCALITY,
            ADDRESS_FORMATTED, ADDRESS_COUNTRY, ADDRESS_REGION, ADDRESS_LOCALITY,
            ADDRESS_POSTAL_CODE, ADDRESS_STREET, ADDRESS_HOUSE_NUMBER, SEX, NATIONALITIES,
            ISSUANCE_DATE, EXPIRY_DATE, ISSUING_AUTHORITY, DOCUMENT_NUMBER, ISSUING_COUNTRY,
            ISSUING_JURISDICTION, PERSONAL_ADMINISTRATIVE_NUMBER, PORTRAIT, EMAIL, PHONE_NUMBER,
            TRUST_ANCHOR,
        )
    }.map {
        NormalizedJsonPath() + it
    },
)
