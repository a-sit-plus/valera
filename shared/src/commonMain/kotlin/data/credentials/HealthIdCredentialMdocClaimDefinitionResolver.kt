package data.credentials

import at.asitplus.wallet.healthid.HealthIdScheme

class HealthIdCredentialMdocClaimDefinitionResolver {
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): HealthIdCredentialClaimDefinition? = with(HealthIdScheme.Attributes) {
        when (namespace) {
            HealthIdScheme.isoNamespace -> when (claimName) {
                HEALTH_INSURANCE_ID -> HealthIdCredentialClaimDefinition.HEALTH_INSURANCE_ID
                PATIENT_ID -> HealthIdCredentialClaimDefinition.PATIENT_ID
                TAX_NUMBER -> HealthIdCredentialClaimDefinition.TAX_NUMBER
                ONE_TIME_TOKEN -> HealthIdCredentialClaimDefinition.ONE_TIME_TOKEN
                E_PRESCRIPTION_CODE -> HealthIdCredentialClaimDefinition.E_PRESCRIPTION_CODE
                AFFILIATION_COUNTRY -> HealthIdCredentialClaimDefinition.AFFILIATION_COUNTRY
                ISSUE_DATE -> HealthIdCredentialClaimDefinition.ISSUE_DATE
                EXPIRY_DATE -> HealthIdCredentialClaimDefinition.EXPIRY_DATE
                ISSUING_AUTHORITY -> HealthIdCredentialClaimDefinition.ISSUING_AUTHORITY
                DOCUMENT_NUMBER -> HealthIdCredentialClaimDefinition.DOCUMENT_NUMBER
                ADMINISTRATIVE_NUMBER -> HealthIdCredentialClaimDefinition.ADMINISTRATIVE_NUMBER
                ISSUING_COUNTRY -> HealthIdCredentialClaimDefinition.ISSUING_COUNTRY
                ISSUING_JURISDICTION -> HealthIdCredentialClaimDefinition.ISSUING_JURISDICTION
                else -> null
            }

            else -> null
        }
    }
}