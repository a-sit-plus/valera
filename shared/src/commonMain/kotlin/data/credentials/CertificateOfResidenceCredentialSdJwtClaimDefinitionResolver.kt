package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements

class CertificateOfResidenceCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(path: NormalizedJsonPath): CertificateOfResidenceCredentialClaimDefinition? {
        return path.segments.firstOrNull()?.let { first ->
            when (first) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
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
                    CertificateOfResidenceDataElements.RESIDENCE_ADDRESS -> with(CertificateOfResidenceDataElements.Address) {
                        when (val second = path.segments.drop(1).firstOrNull()) {
                            is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                                PO_BOX -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_PO_BOX
                                THOROUGHFARE -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_THOROUGHFARE
                                LOCATOR_DESIGNATOR -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR
                                LOCATOR_NAME -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_LOCATOR_NAME
                                POST_CODE -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_POST_CODE
                                POST_NAME -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_POST_NAME
                                ADMIN_UNIT_L_1 -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_ADMIN_UNIT_L_1
                                ADMIN_UNIT_L_2 -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_ADMIN_UNIT_L_2
                                FULL_ADDRESS -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_FULL_ADDRESS
                                else -> null
                            }

                            else -> CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS
                        }
                    }

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
    }
}

