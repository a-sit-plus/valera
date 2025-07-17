package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_affiliation_country
import at.asitplus.valera.resources.attribute_friendly_name_church_tax_id
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_iban
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_pid_id
import at.asitplus.valera.resources.attribute_friendly_name_registered_family_name
import at.asitplus.valera.resources.attribute_friendly_name_registered_given_name
import at.asitplus.valera.resources.attribute_friendly_name_resident_address
import at.asitplus.valera.resources.attribute_friendly_name_tax_number
import at.asitplus.valera.resources.attribute_friendly_name_verification_status
import org.jetbrains.compose.resources.StringResource


class TaxIdCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(
        attributeName: NormalizedJsonPath
    ) = TaxIdCredentialSdJwtClaimDefinitionResolver().resolveOrNull(
        attributeName = attributeName
    )?.stringResource()

    private fun TaxIdCredentialClaimDefinition.stringResource(): StringResource? = when (this) {
        TaxIdCredentialClaimDefinition.TAX_NUMBER -> Res.string.attribute_friendly_name_tax_number
        TaxIdCredentialClaimDefinition.AFFILIATION_COUNTRY -> Res.string.attribute_friendly_name_affiliation_country
        TaxIdCredentialClaimDefinition.REGISTERED_FAMILY_NAME -> Res.string.attribute_friendly_name_registered_family_name
        TaxIdCredentialClaimDefinition.REGISTERED_GIVEN_NAME -> Res.string.attribute_friendly_name_registered_given_name
        TaxIdCredentialClaimDefinition.RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_resident_address
        TaxIdCredentialClaimDefinition.BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
        TaxIdCredentialClaimDefinition.CHURCH_TAX_ID -> Res.string.attribute_friendly_name_church_tax_id
        TaxIdCredentialClaimDefinition.IBAN -> Res.string.attribute_friendly_name_iban
        TaxIdCredentialClaimDefinition.PID_ID -> Res.string.attribute_friendly_name_pid_id
        TaxIdCredentialClaimDefinition.ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
        TaxIdCredentialClaimDefinition.VERIFICATION_STATUS -> Res.string.attribute_friendly_name_verification_status
        TaxIdCredentialClaimDefinition.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
        TaxIdCredentialClaimDefinition.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
        TaxIdCredentialClaimDefinition.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
        TaxIdCredentialClaimDefinition.ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
        TaxIdCredentialClaimDefinition.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
        TaxIdCredentialClaimDefinition.ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
    }
}

