package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_branch
import at.asitplus.valera.resources.attribute_friendly_name_company_activity
import at.asitplus.valera.resources.attribute_friendly_name_company_end_date
import at.asitplus.valera.resources.attribute_friendly_name_company_euid
import at.asitplus.valera.resources.attribute_friendly_name_company_name
import at.asitplus.valera.resources.attribute_friendly_name_company_status
import at.asitplus.valera.resources.attribute_friendly_name_company_type
import at.asitplus.valera.resources.attribute_friendly_name_contact_data
import at.asitplus.valera.resources.attribute_friendly_name_postal_address
import at.asitplus.valera.resources.attribute_friendly_name_registered_address
import at.asitplus.valera.resources.attribute_friendly_name_registration_date
import at.asitplus.valera.resources.attribute_friendly_name_vat_number
import data.credentials.CompanyRegistrationCredentialClaimDefinition.*


object CompanyRegistrationCredentialAttributeTranslator : CredentialAttributeTranslator {
    fun stringResourceOf(
        claimDefinition: CompanyRegistrationCredentialClaimDefinition
    ) = claimDefinition.stringResource()

    fun CompanyRegistrationCredentialClaimDefinition.stringResource() = when (this) {
        COMPANY_NAME -> Res.string.attribute_friendly_name_company_name
        COMPANY_TYPE -> Res.string.attribute_friendly_name_company_type
        COMPANY_STATUS -> Res.string.attribute_friendly_name_company_status
        COMPANY_ACTIVITY -> Res.string.attribute_friendly_name_company_activity
        REGISTRATION_DATE -> Res.string.attribute_friendly_name_registration_date
        COMPANY_END_DATE -> Res.string.attribute_friendly_name_company_end_date
        COMPANY_EUID -> Res.string.attribute_friendly_name_company_euid
        VAT_NUMBER -> Res.string.attribute_friendly_name_vat_number
        COMPANY_CONTACT_DATA -> Res.string.attribute_friendly_name_contact_data
        REGISTERED_ADDRESS -> Res.string.attribute_friendly_name_registered_address
        POSTAL_ADDRESS -> Res.string.attribute_friendly_name_postal_address
        BRANCH -> Res.string.attribute_friendly_name_branch
    }

    fun translate(claimReference: SingleClaimReference) = when (claimReference) {
        is MdocClaimReference -> CompanyRegistrationCredentialMdocClaimDefinitionResolver().resolveOrNull(
            namespace = claimReference.namespace,
            claimName = claimReference.claimName,
        )

        is SdJwtClaimReference -> CompanyRegistrationCredentialSdJwtClaimDefinitionResolver().resolveOrNull(
            claimReference.normalizedJsonPath
        )
    }?.stringResource()

    override fun translate(
        attributeName: NormalizedJsonPath,
    ) = CompanyRegistrationCredentialClaimDefinitionResolver().resolveOrNull(
        SdJwtClaimReference(attributeName)
    )?.stringResource()
}