package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_authentic_source
import at.asitplus.valera.resources.attribute_friendly_name_authentic_source_id
import at.asitplus.valera.resources.attribute_friendly_name_authentic_source_name
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_effective_from_date
import at.asitplus.valera.resources.attribute_friendly_name_effective_until_date
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority_id
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority_name
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_personal_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_social_security_number

class EhicCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(
        attributeName: NormalizedJsonPath
    ) = EhicCredentialSdJwtClaimDefinitionResolver().resolveOrNull(attributeName)?.stringResource()

    private fun EhicCredentialClaimDefinition.stringResource() = when (this) {
        EhicCredentialClaimDefinition.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
        EhicCredentialClaimDefinition.SOCIAL_SECURITY_NUMBER -> Res.string.attribute_friendly_name_social_security_number
        EhicCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_personal_administrative_number
        EhicCredentialClaimDefinition.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
        EhicCredentialClaimDefinition.ISSUING_AUTHORITY_ID -> Res.string.attribute_friendly_name_issuing_authority_id
        EhicCredentialClaimDefinition.ISSUING_AUTHORITY_NAME -> Res.string.attribute_friendly_name_issuing_authority_name
        EhicCredentialClaimDefinition.AUTHENTIC_SOURCE -> Res.string.attribute_friendly_name_authentic_source
        EhicCredentialClaimDefinition.AUTHENTIC_SOURCE_ID -> Res.string.attribute_friendly_name_authentic_source_id
        EhicCredentialClaimDefinition.AUTHENTIC_SOURCE_NAME -> Res.string.attribute_friendly_name_authentic_source_name
        EhicCredentialClaimDefinition.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
        EhicCredentialClaimDefinition.ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
        EhicCredentialClaimDefinition.DATE_OF_ISSUANCE -> Res.string.attribute_friendly_name_issue_date
        EhicCredentialClaimDefinition.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
        EhicCredentialClaimDefinition.DATE_OF_EXPIRY -> Res.string.attribute_friendly_name_expiry_date
        EhicCredentialClaimDefinition.STARTING_DATE -> Res.string.attribute_friendly_name_effective_from_date
        EhicCredentialClaimDefinition.ENDING_DATE -> Res.string.attribute_friendly_name_effective_until_date
    }
}