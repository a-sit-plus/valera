package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import data.PersonalDataCategory

object CertificateOfResidenceCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
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
            CertificateOfResidenceDataElements.ARRIVAL_DATE
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = CertificateOfResidenceDataElements.ALL_ELEMENTS.map {
        NormalizedJsonPath() + it
    },
)