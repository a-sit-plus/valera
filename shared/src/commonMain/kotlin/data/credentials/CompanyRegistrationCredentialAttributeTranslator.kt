package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
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
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.BRANCH
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_ACTIVITY
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_CONTACT_DATA
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_END_DATE
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_EUID
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_NAME
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_STATUS
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_TYPE
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.POSTAL_ADDRESS
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.REGISTERED_ADDRESS
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.REGISTRATION_DATE
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.VAT_NUMBER
import org.jetbrains.compose.resources.StringResource


object CompanyRegistrationCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
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
                else -> null
            }

            else -> null
        }
}