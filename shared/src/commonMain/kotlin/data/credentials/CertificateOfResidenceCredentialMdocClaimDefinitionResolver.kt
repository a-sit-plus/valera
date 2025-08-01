package data.credentials

import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme

class CertificateOfResidenceCredentialMdocClaimDefinitionResolver {
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): CertificateOfResidenceCredentialClaimDefinition? = when (namespace) {
        CertificateOfResidenceScheme.isoNamespace -> when (claimName) {
            CertificateOfResidenceDataElements.ADMINISTRATIVE_NUMBER -> CertificateOfResidenceCredentialClaimDefinition.ADMINISTRATIVE_NUMBER
            CertificateOfResidenceDataElements.ISSUANCE_DATE -> CertificateOfResidenceCredentialClaimDefinition.ISSUANCE_DATE
            CertificateOfResidenceDataElements.EXPIRY_DATE -> CertificateOfResidenceCredentialClaimDefinition.EXPIRY_DATE
            CertificateOfResidenceDataElements.ISSUING_AUTHORITY -> CertificateOfResidenceCredentialClaimDefinition.ISSUING_AUTHORITY
            CertificateOfResidenceDataElements.DOCUMENT_NUMBER -> CertificateOfResidenceCredentialClaimDefinition.DOCUMENT_NUMBER
            CertificateOfResidenceDataElements.ISSUING_COUNTRY -> CertificateOfResidenceCredentialClaimDefinition.ISSUING_COUNTRY
            CertificateOfResidenceDataElements.ISSUING_JURISDICTION -> CertificateOfResidenceCredentialClaimDefinition.ISSUING_JURISDICTION
            CertificateOfResidenceDataElements.FAMILY_NAME -> CertificateOfResidenceCredentialClaimDefinition.FAMILY_NAME
            CertificateOfResidenceDataElements.GIVEN_NAME -> CertificateOfResidenceCredentialClaimDefinition.GIVEN_NAME
            CertificateOfResidenceDataElements.BIRTH_DATE -> CertificateOfResidenceCredentialClaimDefinition.BIRTH_DATE
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_PO_BOX -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_PO_BOX
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_THOROUGHFARE -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_THOROUGHFARE
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_LOCATOR_NAME -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_LOCATOR_NAME
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_POST_CODE -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_POST_CODE
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_POST_NAME -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_POST_NAME
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_ADMIN_UNIT_L_1 -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_ADMIN_UNIT_L_1
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_ADMIN_UNIT_L_2 -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_ADMIN_UNIT_L_2
            CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_FULL_ADDRESS -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_FULL_ADDRESS
            CertificateOfResidenceDataElements.GENDER -> CertificateOfResidenceCredentialClaimDefinition.GENDER
            CertificateOfResidenceDataElements.BIRTH_PLACE -> CertificateOfResidenceCredentialClaimDefinition.BIRTH_PLACE
            CertificateOfResidenceDataElements.ARRIVAL_DATE -> CertificateOfResidenceCredentialClaimDefinition.ARRIVAL_DATE
            CertificateOfResidenceDataElements.NATIONALITY -> CertificateOfResidenceCredentialClaimDefinition.NATIONALITY
            else -> null
        }

        else -> null
    }
}