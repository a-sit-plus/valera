package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import data.PersonalDataCategory

object CertificateOfResidenceCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to with(CertificateOfResidenceDataElements) {
            listOf(
                GIVEN_NAME,
                FAMILY_NAME,
                BIRTH_DATE,
                NATIONALITY,
                BIRTH_PLACE,
                GENDER,
            ).map { NormalizedJsonPath() + it to null }
        },

        PersonalDataCategory.ResidenceData to with(CertificateOfResidenceDataElements.Address) {
            listOf(
                PO_BOX,
                THOROUGHFARE,
                LOCATOR_DESIGNATOR,
                LOCATOR_NAME,
                POST_CODE,
                POST_NAME,
                ADMIN_UNIT_L_1,
                ADMIN_UNIT_L_2,
                FULL_ADDRESS,
            ).map { NormalizedJsonPath(NormalizedJsonPathSegment.NameSegment(CertificateOfResidenceDataElements.RESIDENCE_ADDRESS)) + it to null }
        },
        PersonalDataCategory.Metadata to with(CertificateOfResidenceDataElements) {
            listOf(
                ISSUANCE_DATE,
                EXPIRY_DATE,
                ISSUING_COUNTRY,
                ISSUING_AUTHORITY,
                ISSUING_JURISDICTION,
                DOCUMENT_NUMBER,
                ADMINISTRATIVE_NUMBER,
                ARRIVAL_DATE
            ).map { NormalizedJsonPath() + it to null }
        },
    ),
    allAttributes = CertificateOfResidenceDataElements.ALL_ELEMENTS.map {
        NormalizedJsonPath() + it
    },
)
