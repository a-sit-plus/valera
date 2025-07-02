package data.credentials

import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme

class CompanyRegistrationCredentialMdocClaimDefinitionResolver {
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ) = when(namespace) {
        CompanyRegistrationScheme.isoNamespace -> when(claimName) {
            CompanyRegistrationDataElements.COMPANY_NAME -> CompanyRegistrationCredentialClaimDefinition.COMPANY_NAME
            CompanyRegistrationDataElements.COMPANY_TYPE -> CompanyRegistrationCredentialClaimDefinition.COMPANY_TYPE
            CompanyRegistrationDataElements.COMPANY_STATUS -> CompanyRegistrationCredentialClaimDefinition.COMPANY_STATUS
            CompanyRegistrationDataElements.COMPANY_ACTIVITY -> CompanyRegistrationCredentialClaimDefinition.COMPANY_ACTIVITY
            CompanyRegistrationDataElements.REGISTRATION_DATE -> CompanyRegistrationCredentialClaimDefinition.REGISTRATION_DATE
            CompanyRegistrationDataElements.COMPANY_END_DATE -> CompanyRegistrationCredentialClaimDefinition.COMPANY_END_DATE
            CompanyRegistrationDataElements.COMPANY_EUID -> CompanyRegistrationCredentialClaimDefinition.COMPANY_EUID
            CompanyRegistrationDataElements.VAT_NUMBER -> CompanyRegistrationCredentialClaimDefinition.VAT_NUMBER
            CompanyRegistrationDataElements.COMPANY_CONTACT_DATA -> CompanyRegistrationCredentialClaimDefinition.COMPANY_CONTACT_DATA
            CompanyRegistrationDataElements.REGISTERED_ADDRESS -> CompanyRegistrationCredentialClaimDefinition.REGISTERED_ADDRESS
            CompanyRegistrationDataElements.POSTAL_ADDRESS -> CompanyRegistrationCredentialClaimDefinition.POSTAL_ADDRESS
            CompanyRegistrationDataElements.BRANCH -> CompanyRegistrationCredentialClaimDefinition.BRANCH
            else -> null
        }

        else -> null
    }
}