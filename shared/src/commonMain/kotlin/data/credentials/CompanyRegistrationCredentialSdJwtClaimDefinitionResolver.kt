package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements

class CompanyRegistrationCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(path: NormalizedJsonPath): CompanyRegistrationCredentialClaimDefinition? {
        return path.segments.firstOrNull()?.let { first ->
            when (first) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
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
    }
}
