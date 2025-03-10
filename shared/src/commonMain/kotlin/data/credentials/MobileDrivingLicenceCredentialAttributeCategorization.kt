package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import data.PersonalDataCategory

object MobileDrivingLicenceCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to listOf(
            MobileDrivingLicenceDataElements.GIVEN_NAME,
            MobileDrivingLicenceDataElements.FAMILY_NAME,
            MobileDrivingLicenceDataElements.GIVEN_NAME_NATIONAL_CHARACTER,
            MobileDrivingLicenceDataElements.FAMILY_NAME_NATIONAL_CHARACTER,
            MobileDrivingLicenceDataElements.BIRTH_DATE,
            MobileDrivingLicenceDataElements.PORTRAIT,
            MobileDrivingLicenceDataElements.PORTRAIT_CAPTURE_DATE,
            MobileDrivingLicenceDataElements.NATIONALITY,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.AgeData to listOf(
            MobileDrivingLicenceDataElements.AGE_OVER_12,
            MobileDrivingLicenceDataElements.AGE_OVER_14,
            MobileDrivingLicenceDataElements.AGE_OVER_16,
            MobileDrivingLicenceDataElements.AGE_OVER_18,
            MobileDrivingLicenceDataElements.AGE_OVER_21,
            MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR,
            MobileDrivingLicenceDataElements.AGE_IN_YEARS,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.BirthData to listOf(
            MobileDrivingLicenceDataElements.BIRTH_PLACE,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.ResidenceData to listOf(
            MobileDrivingLicenceDataElements.RESIDENT_ADDRESS,
            MobileDrivingLicenceDataElements.RESIDENT_CITY,
            MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE,
            MobileDrivingLicenceDataElements.RESIDENT_COUNTRY,
            MobileDrivingLicenceDataElements.RESIDENT_STATE,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.AppearanceData to listOf(
            MobileDrivingLicenceDataElements.SEX,
            MobileDrivingLicenceDataElements.HEIGHT,
            MobileDrivingLicenceDataElements.WEIGHT,
            MobileDrivingLicenceDataElements.EYE_COLOUR,
            MobileDrivingLicenceDataElements.HAIR_COLOUR,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.DrivingPermissions to listOf(
            MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES, // TODO: extract data
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.BiometricData to listOf(
            MobileDrivingLicenceDataElements.SIGNATURE_USUAL_MARK, // TODO: extract data
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.Metadata to listOf(
            MobileDrivingLicenceDataElements.DOCUMENT_NUMBER,
            MobileDrivingLicenceDataElements.ISSUE_DATE,
            MobileDrivingLicenceDataElements.EXPIRY_DATE,
            MobileDrivingLicenceDataElements.ISSUING_COUNTRY,
            MobileDrivingLicenceDataElements.ISSUING_AUTHORITY,
            MobileDrivingLicenceDataElements.ISSUING_JURISDICTION,
            MobileDrivingLicenceDataElements.ADMINISTRATIVE_NUMBER,
            MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN, // TODO: extract data
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = MobileDrivingLicenceDataElements.ALL_ELEMENTS.map {
        NormalizedJsonPath() + it
    },
)
