package data

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.credentials.IdAustriaCredentialMainAddress

/**
 * I'm sorry, this got a little overloaded..
 *
 * This should centralize credentials displayed in the view where attributes to be loaded are selected, and the generic credential view.
 * The usage of the outer type Map<CredentialScheme, Map<PersonalDataCategory, ...>> should therefore be clear.
 *
 * The inner type, `List<Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>>`, is there to
 *  separate between the attribute to be requested (which is the first entry in the pair) and the subfields (e.g. if the field itself is an encoded json object) to be displayed in the generic data view (if any, otherwise null)
 */
val credentialAttributeCategorization: Map<ConstantIndex.CredentialScheme, Map<PersonalDataCategory, List<Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>>>> =
    mapOf(
        IdAustriaScheme to mapOf(
            PersonalDataCategory.IdentityData to listOf(
                IdAustriaScheme.Attributes.FIRSTNAME,
                IdAustriaScheme.Attributes.LASTNAME,
                IdAustriaScheme.Attributes.DATE_OF_BIRTH,
                IdAustriaScheme.Attributes.PORTRAIT,
            ).map { NormalizedJsonPath() + it to null },

            PersonalDataCategory.AgeData to listOf(
                IdAustriaScheme.Attributes.AGE_OVER_14,
                IdAustriaScheme.Attributes.AGE_OVER_16,
                IdAustriaScheme.Attributes.AGE_OVER_18,
                IdAustriaScheme.Attributes.AGE_OVER_21,
            ).map { NormalizedJsonPath() + it to null },

            PersonalDataCategory.ResidenceData to listOf(
                (NormalizedJsonPath() + IdAustriaScheme.Attributes.MAIN_ADDRESS) to listOf(
                    IdAustriaCredentialMainAddress.STRASSE,
                    IdAustriaCredentialMainAddress.HAUSNUMMER,
                    IdAustriaCredentialMainAddress.STIEGE,
                    IdAustriaCredentialMainAddress.TUER,
                    IdAustriaCredentialMainAddress.POSTLEITZAHL,
                    IdAustriaCredentialMainAddress.ORTSCHAFT,
                    IdAustriaCredentialMainAddress.GEMEINDEBEZEICHNUNG,
                    IdAustriaCredentialMainAddress.GEMEINDEKENNZIFFER,
                ).map { NormalizedJsonPath() + it },
            ),

            PersonalDataCategory.AdmissionData to listOf(
                IdAustriaScheme.Attributes.VEHICLE_REGISTRATION, // TODO: Extract data
            ).map { NormalizedJsonPath() + it to null },
        ).withOthersFrom(IdAustriaScheme.claimNames.map { NormalizedJsonPath(NameSegment(it)) }),

        EuPidScheme to mapOf(
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
        ).withOthersFrom(EuPidScheme.claimNames.map {
            NormalizedJsonPath() + it
        }),

        MobileDrivingLicenceScheme to mapOf(
            PersonalDataCategory.IdentityData to listOf(
                MobileDrivingLicenceDataElements.GIVEN_NAME,
                MobileDrivingLicenceDataElements.FAMILY_NAME,
                MobileDrivingLicenceDataElements.GIVEN_NAME_NATIONAL_CHARACTER,
                MobileDrivingLicenceDataElements.FAMILY_NAME_NATIONAL_CHARACTER,
                MobileDrivingLicenceDataElements.BIRTH_DATE,
                MobileDrivingLicenceDataElements.PORTRAIT,
                MobileDrivingLicenceDataElements.PORTRAIT_CAPTURE_DATE,

                MobileDrivingLicenceDataElements.NATIONALITY, // TODO: not in figma?
            ).map { NormalizedJsonPath() + it to null },

            PersonalDataCategory.AgeData to listOf(
                MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR,
                MobileDrivingLicenceDataElements.AGE_IN_YEARS,
                MobileDrivingLicenceDataElements.AGE_OVER_18,
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
                MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN, // TODO: extract data
            ).map { NormalizedJsonPath() + it to null },

            PersonalDataCategory.Metadata to listOf(
                MobileDrivingLicenceDataElements.ISSUE_DATE,
                MobileDrivingLicenceDataElements.EXPIRY_DATE,
                MobileDrivingLicenceDataElements.ISSUING_COUNTRY,
                MobileDrivingLicenceDataElements.ISSUING_AUTHORITY,
                MobileDrivingLicenceDataElements.ISSUING_JURISDICTION,
                MobileDrivingLicenceDataElements.DOCUMENT_NUMBER,
                MobileDrivingLicenceDataElements.ADMINISTRATIVE_NUMBER,
            ).map { NormalizedJsonPath() + it to null },
        ).withOthersFrom(MobileDrivingLicenceDataElements.ALL_ELEMENTS.map {
            NormalizedJsonPath() + it
        }),

        PowerOfRepresentationScheme to mapOf(
            PersonalDataCategory.RepresentationData to listOf(
                PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER,
                PowerOfRepresentationDataElements.LEGAL_NAME,
                PowerOfRepresentationDataElements.FULL_POWERS,
                PowerOfRepresentationDataElements.E_SERVICE,
                PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE,
                PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE,
            ).map { NormalizedJsonPath() + it to null },

            PersonalDataCategory.Metadata to listOf(
                PowerOfRepresentationDataElements.ISSUANCE_DATE,
                PowerOfRepresentationDataElements.EXPIRY_DATE,
                PowerOfRepresentationDataElements.ISSUING_COUNTRY,
                PowerOfRepresentationDataElements.ISSUING_AUTHORITY,
                PowerOfRepresentationDataElements.ISSUING_JURISDICTION,
                PowerOfRepresentationDataElements.DOCUMENT_NUMBER,
                PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER,
            ).map { NormalizedJsonPath() + it to null },
        ).withOthersFrom(PowerOfRepresentationDataElements.ALL_ELEMENTS.map {
            NormalizedJsonPath() + it
        }),

        CertificateOfResidenceScheme to mapOf(
            PersonalDataCategory.IdentityData to listOf(
                CertificateOfResidenceDataElements.GIVEN_NAME,
                CertificateOfResidenceDataElements.FAMILY_NAME,
                CertificateOfResidenceDataElements.BIRTH_DATE,
                CertificateOfResidenceDataElements.NATIONALITY,
                CertificateOfResidenceDataElements.BIRTH_PLACE,
                CertificateOfResidenceDataElements.GENDER,
            ).map { NormalizedJsonPath() + it to null },

            PersonalDataCategory.ResidenceData to listOf(
                NormalizedJsonPath() + CertificateOfResidenceDataElements.RESIDENCE_ADDRESS to listOf<NormalizedJsonPath>(
                    // TODO: allow display of child elements
                ),
            ),
            PersonalDataCategory.Metadata to listOf(
                CertificateOfResidenceDataElements.ISSUANCE_DATE,
                CertificateOfResidenceDataElements.EXPIRY_DATE,
                CertificateOfResidenceDataElements.ISSUING_COUNTRY,
                CertificateOfResidenceDataElements.ISSUING_AUTHORITY,
                CertificateOfResidenceDataElements.ISSUING_JURISDICTION,
                CertificateOfResidenceDataElements.DOCUMENT_NUMBER,
                CertificateOfResidenceDataElements.ADMINISTRATIVE_NUMBER,
            ).map { NormalizedJsonPath() + it to null },
        ).withOthersFrom(CertificateOfResidenceDataElements.ALL_ELEMENTS.map {
            NormalizedJsonPath() + it
        }),
    )

private fun Map<PersonalDataCategory, List<Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>>>.withOthersFrom(
    allAttributes: Collection<NormalizedJsonPath>
): Map<PersonalDataCategory, List<Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>>> {
    val categorization = this
    val categorizedAttributes = categorization.map { it.value.map { it.first } }.flatten()
    val otherAttributes = allAttributes.filterNot { uncategorized ->
        categorizedAttributes.any { categorized ->
            categorized.toString() == uncategorized.toString()
        }
    }.map {
        NormalizedJsonPath() + it to (null as List<NormalizedJsonPath>?)
    }
    return categorization + (if (otherAttributes.isEmpty()) listOf() else listOf(
        PersonalDataCategory.OtherData to otherAttributes,
    ))
}
