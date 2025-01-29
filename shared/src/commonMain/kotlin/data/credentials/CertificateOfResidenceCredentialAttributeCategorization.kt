package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
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

        PersonalDataCategory.ResidenceData to with(CertificateOfResidenceDataElements) {
            listOf(
                RESIDENCE_ADDRESS_PO_BOX,
                RESIDENCE_ADDRESS_THOROUGHFARE,
                RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR,
                RESIDENCE_ADDRESS_LOCATOR_NAME,
                RESIDENCE_ADDRESS_POST_CODE,
                RESIDENCE_ADDRESS_POST_NAME,
                RESIDENCE_ADDRESS_ADMIN_UNIT_L_1,
                RESIDENCE_ADDRESS_ADMIN_UNIT_L_2,
                RESIDENCE_ADDRESS_FULL_ADDRESS,
            ).map { NormalizedJsonPath() + it to null }
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
