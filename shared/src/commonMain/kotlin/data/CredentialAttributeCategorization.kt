package data

import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationScheme

val credentialAttributeCategorization = mapOf(
    IdAustriaScheme to listOf(
        PersonalDataCategory.IdentityData to listOf(
            IdAustriaScheme.Attributes.FIRSTNAME,
            IdAustriaScheme.Attributes.LASTNAME,
            IdAustriaScheme.Attributes.DATE_OF_BIRTH,
            IdAustriaScheme.Attributes.PORTRAIT,
        ),
        PersonalDataCategory.AgeData to listOf(
            IdAustriaScheme.Attributes.AGE_OVER_14,
            IdAustriaScheme.Attributes.AGE_OVER_16,
            IdAustriaScheme.Attributes.AGE_OVER_18,
            IdAustriaScheme.Attributes.AGE_OVER_21,
        ),
        PersonalDataCategory.ResidenceData to listOf(
            IdAustriaScheme.Attributes.MAIN_ADDRESS,
        ),
        PersonalDataCategory.AdmissionData to listOf(
            IdAustriaScheme.Attributes.VEHICLE_REGISTRATION, // TODO: Extract data
        ),
    ).withOthersFrom(IdAustriaScheme.claimNames),

    EuPidScheme to listOf(
        PersonalDataCategory.IdentityData to listOf(
            EuPidScheme.Attributes.GIVEN_NAME,
            EuPidScheme.Attributes.FAMILY_NAME,
            EuPidScheme.Attributes.BIRTH_DATE,
            EuPidScheme.Attributes.NATIONALITY,

            EuPidScheme.Attributes.GENDER,
        ),
        PersonalDataCategory.AgeData to listOf(
            EuPidScheme.Attributes.AGE_OVER_18,
            EuPidScheme.Attributes.AGE_BIRTH_YEAR,
            EuPidScheme.Attributes.AGE_IN_YEARS,
        ),
        PersonalDataCategory.BirthData to listOf(
            EuPidScheme.Attributes.GIVEN_NAME_BIRTH,
            EuPidScheme.Attributes.FAMILY_NAME_BIRTH,
            EuPidScheme.Attributes.BIRTH_PLACE,
            EuPidScheme.Attributes.BIRTH_CITY,
            EuPidScheme.Attributes.BIRTH_COUNTRY,
            EuPidScheme.Attributes.BIRTH_STATE,
        ),
        PersonalDataCategory.ResidenceData to listOf(
            EuPidScheme.Attributes.RESIDENT_ADDRESS,
            EuPidScheme.Attributes.RESIDENT_COUNTRY,
            EuPidScheme.Attributes.RESIDENT_STATE,
            EuPidScheme.Attributes.RESIDENT_CITY,
            EuPidScheme.Attributes.RESIDENT_POSTAL_CODE,
            EuPidScheme.Attributes.RESIDENT_STREET,
            EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER,
        ),
    ).withOthersFrom(EuPidScheme.claimNames),

    MobileDrivingLicenceScheme to listOf(
        PersonalDataCategory.IdentityData to listOf(
            MobileDrivingLicenceDataElements.GIVEN_NAME,
            MobileDrivingLicenceDataElements.FAMILY_NAME,
            MobileDrivingLicenceDataElements.GIVEN_NAME_NATIONAL_CHARACTER,
            MobileDrivingLicenceDataElements.FAMILY_NAME_NATIONAL_CHARACTER,
            MobileDrivingLicenceDataElements.BIRTH_DATE,
            MobileDrivingLicenceDataElements.PORTRAIT,
            MobileDrivingLicenceDataElements.PORTRAIT_CAPTURE_DATE,

            MobileDrivingLicenceDataElements.NATIONALITY, // TODO: not in figma?
        ),
        PersonalDataCategory.AgeData to listOf(
            MobileDrivingLicenceDataElements.AGE_OVER_18,
            MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR,
            MobileDrivingLicenceDataElements.AGE_IN_YEARS,
        ),
        PersonalDataCategory.BirthData to listOf(
            MobileDrivingLicenceDataElements.BIRTH_PLACE,
        ),
        PersonalDataCategory.ResidenceData to listOf(
            MobileDrivingLicenceDataElements.RESIDENT_ADDRESS,
            MobileDrivingLicenceDataElements.RESIDENT_CITY,
            MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE,
            MobileDrivingLicenceDataElements.RESIDENT_COUNTRY,
            MobileDrivingLicenceDataElements.RESIDENT_STATE,
        ),
        PersonalDataCategory.AppearanceData to listOf(
            MobileDrivingLicenceDataElements.SEX,
            MobileDrivingLicenceDataElements.HEIGHT,
            MobileDrivingLicenceDataElements.WEIGHT,
            MobileDrivingLicenceDataElements.EYE_COLOUR,
            MobileDrivingLicenceDataElements.HAIR_COLOUR,
        ),
        PersonalDataCategory.DrivingPermissions to listOf(
            MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES, // TODO: extract data
        ),
        PersonalDataCategory.BiometricData to listOf(
            MobileDrivingLicenceDataElements.SIGNATURE_USUAL_MARK, // TODO: extract data
            MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN, // TODO: extract data
        ),
        PersonalDataCategory.Metadata to listOf(
            MobileDrivingLicenceDataElements.ISSUE_DATE,
            MobileDrivingLicenceDataElements.EXPIRY_DATE,
            MobileDrivingLicenceDataElements.ISSUING_COUNTRY,
            MobileDrivingLicenceDataElements.ISSUING_AUTHORITY,
            MobileDrivingLicenceDataElements.ISSUING_JURISDICTION,
            MobileDrivingLicenceDataElements.DOCUMENT_NUMBER,
            MobileDrivingLicenceDataElements.ADMINISTRATIVE_NUMBER,
        ),
    ).withOthersFrom(MobileDrivingLicenceDataElements.ALL_ELEMENTS),

    PowerOfRepresentationScheme to listOf(
        PersonalDataCategory.RepresentationData to listOf(
            PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER,
            PowerOfRepresentationDataElements.LEGAL_NAME,
            PowerOfRepresentationDataElements.FULL_POWERS,
            PowerOfRepresentationDataElements.E_SERVICE,
            PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE,
            PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE,
        ),
        PersonalDataCategory.Metadata to listOf(
            PowerOfRepresentationDataElements.ISSUANCE_DATE,
            PowerOfRepresentationDataElements.EXPIRY_DATE,
            PowerOfRepresentationDataElements.ISSUING_COUNTRY,
            PowerOfRepresentationDataElements.ISSUING_AUTHORITY,
            PowerOfRepresentationDataElements.ISSUING_JURISDICTION,
            PowerOfRepresentationDataElements.DOCUMENT_NUMBER,
            PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER,
        ),
    ).withOthersFrom(PowerOfRepresentationDataElements.ALL_ELEMENTS),

    CertificateOfResidenceScheme to listOf(
        PersonalDataCategory.IdentityData to listOf(
            CertificateOfResidenceDataElements.GIVEN_NAME,
            CertificateOfResidenceDataElements.FAMILY_NAME,
            CertificateOfResidenceDataElements.BIRTH_DATE,
            CertificateOfResidenceDataElements.NATIONALITY,
            CertificateOfResidenceDataElements.BIRTH_PLACE,
            CertificateOfResidenceDataElements.GENDER,
        ),
        PersonalDataCategory.ResidenceData to listOf(
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS, // TODO: allow display of child elements
        ),
        PersonalDataCategory.Metadata to listOf(
            CertificateOfResidenceDataElements.ISSUANCE_DATE,
            CertificateOfResidenceDataElements.EXPIRY_DATE,
            CertificateOfResidenceDataElements.ISSUING_COUNTRY,
            CertificateOfResidenceDataElements.ISSUING_AUTHORITY,
            CertificateOfResidenceDataElements.ISSUING_JURISDICTION,
            CertificateOfResidenceDataElements.DOCUMENT_NUMBER,
            CertificateOfResidenceDataElements.ADMINISTRATIVE_NUMBER,
        ),
    ).withOthersFrom(CertificateOfResidenceDataElements.ALL_ELEMENTS),
)

private fun List<Pair<PersonalDataCategory, List<String>>>.withOthersFrom(allAttributes: Collection<String>): List<Pair<PersonalDataCategory, List<String>>> {
    val categorization = this
    val categorizedAttributes = categorization.map { it.second }.flatten()
    val otherAttributes = allAttributes.filterNot {
        categorizedAttributes.contains(it)
    }
    return categorization + if (otherAttributes.isEmpty()) listOf() else listOf(
        PersonalDataCategory.OtherData to otherAttributes,
    )
}