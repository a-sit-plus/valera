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
    ).withOthersFrom(IdAustriaScheme.claimNames),

    EuPidScheme to listOf(
        PersonalDataCategory.IdentityData to listOf(
            EuPidScheme.Attributes.GIVEN_NAME,
            EuPidScheme.Attributes.GIVEN_NAME_BIRTH,
            EuPidScheme.Attributes.FAMILY_NAME,
            EuPidScheme.Attributes.FAMILY_NAME_BIRTH,
            EuPidScheme.Attributes.BIRTH_DATE,
            EuPidScheme.Attributes.BIRTH_PLACE,
            EuPidScheme.Attributes.BIRTH_CITY,
            EuPidScheme.Attributes.BIRTH_COUNTRY,
            EuPidScheme.Attributes.BIRTH_STATE,
            EuPidScheme.Attributes.AGE_BIRTH_YEAR,
            EuPidScheme.Attributes.AGE_IN_YEARS,
            EuPidScheme.Attributes.GENDER,
            EuPidScheme.Attributes.NATIONALITY,
        ),
        PersonalDataCategory.AgeData to listOf(
            EuPidScheme.Attributes.AGE_OVER_18,
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
            MobileDrivingLicenceDataElements.BIRTH_DATE,
            MobileDrivingLicenceDataElements.BIRTH_PLACE,
            MobileDrivingLicenceDataElements.NATIONALITY,
            MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR,
            MobileDrivingLicenceDataElements.AGE_IN_YEARS,
            MobileDrivingLicenceDataElements.PORTRAIT,
        ),
        PersonalDataCategory.AgeData to listOf(
            MobileDrivingLicenceDataElements.AGE_OVER_18,
        ),
        PersonalDataCategory.ResidenceData to listOf(
            MobileDrivingLicenceDataElements.RESIDENT_ADDRESS,
            MobileDrivingLicenceDataElements.RESIDENT_CITY,
            MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE,
            MobileDrivingLicenceDataElements.RESIDENT_COUNTRY,
            MobileDrivingLicenceDataElements.RESIDENT_STATE,
        ),
    ).withOthersFrom(MobileDrivingLicenceDataElements.ALL_ELEMENTS),

    PowerOfRepresentationScheme to listOf(
        PersonalDataCategory.ResidenceData to listOf(
            MobileDrivingLicenceDataElements.RESIDENT_ADDRESS,
            MobileDrivingLicenceDataElements.RESIDENT_CITY,
            MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE,
            MobileDrivingLicenceDataElements.RESIDENT_COUNTRY,
            MobileDrivingLicenceDataElements.RESIDENT_STATE,
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
        )
    ).withOthersFrom(CertificateOfResidenceDataElements.ALL_ELEMENTS),
)

private fun List<Pair<PersonalDataCategory, List<String>>>.withOthersFrom(allAttributes: Collection<String>): List<Pair<PersonalDataCategory, List<String>>> {
    val categorization = this
    val categorizedAttributes = categorization.map { it.second }.flatten()
    val otherAttributes = allAttributes.filterNot {
        categorizedAttributes.contains(it)
    }
    return categorization + if(otherAttributes.isEmpty()) listOf() else listOf(
        PersonalDataCategory.OtherData to otherAttributes,
    )
}