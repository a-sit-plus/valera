package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_e_service
import at.asitplus.valera.resources.attribute_friendly_name_effective_from_date
import at.asitplus.valera.resources.attribute_friendly_name_effective_until_date
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_full_powers
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_legal_name
import at.asitplus.valera.resources.attribute_friendly_name_legal_person_identifier
import org.jetbrains.compose.resources.StringResource


class PowerOfRepresentationCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(
        attributeName: NormalizedJsonPath
    ) = PowerOfRepresentationCredentialSdJwtClaimDefinitionResolver().resolveOrNull(
        attributeName
    )?.stringResource()

    private fun PowerOfRepresentationCredentialClaimDefinition.stringResource(): StringResource? = when (this) {
        PowerOfRepresentationCredentialClaimDefinition.LEGAL_PERSON_IDENTIFIER -> Res.string.attribute_friendly_name_legal_person_identifier
        PowerOfRepresentationCredentialClaimDefinition.LEGAL_NAME -> Res.string.attribute_friendly_name_legal_name
        PowerOfRepresentationCredentialClaimDefinition.FULL_POWERS -> Res.string.attribute_friendly_name_full_powers
        PowerOfRepresentationCredentialClaimDefinition.E_SERVICE -> Res.string.attribute_friendly_name_e_service
        PowerOfRepresentationCredentialClaimDefinition.EFFECTIVE_FROM_DATE -> Res.string.attribute_friendly_name_effective_from_date
        PowerOfRepresentationCredentialClaimDefinition.EFFECTIVE_UNTIL_DATE -> Res.string.attribute_friendly_name_effective_until_date
        PowerOfRepresentationCredentialClaimDefinition.ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
        PowerOfRepresentationCredentialClaimDefinition.ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
        PowerOfRepresentationCredentialClaimDefinition.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
        PowerOfRepresentationCredentialClaimDefinition.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
        PowerOfRepresentationCredentialClaimDefinition.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
        PowerOfRepresentationCredentialClaimDefinition.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
        PowerOfRepresentationCredentialClaimDefinition.ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
    }
}

