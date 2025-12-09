package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_affiliation_country
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_e_prescription_code
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_health_insurance_id
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_one_time_token
import at.asitplus.valera.resources.attribute_friendly_name_patient_id
import at.asitplus.valera.resources.attribute_friendly_name_tax_number
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.app.common.minus
import at.asitplus.wallet.healthid.HealthIdScheme
import org.jetbrains.compose.resources.StringResource


class HealthIdCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(
        attributeName: NormalizedJsonPath
    ) =
        attributeName.minus(HealthIdScheme.isoNamespace).let {
            it.memberName(0)?.let { claim ->
                HealthIdCredentialMdocClaimDefinitionResolver().resolveOrNull(HealthIdScheme.isoNamespace, claim)
                    ?.stringResource()
            }
        }

    private fun HealthIdCredentialClaimDefinition.stringResource(): StringResource? = when (this) {
        HealthIdCredentialClaimDefinition.HEALTH_INSURANCE_ID -> Res.string.attribute_friendly_name_health_insurance_id
        HealthIdCredentialClaimDefinition.PATIENT_ID -> Res.string.attribute_friendly_name_patient_id
        HealthIdCredentialClaimDefinition.TAX_NUMBER -> Res.string.attribute_friendly_name_tax_number
        HealthIdCredentialClaimDefinition.ONE_TIME_TOKEN -> Res.string.attribute_friendly_name_one_time_token
        HealthIdCredentialClaimDefinition.E_PRESCRIPTION_CODE -> Res.string.attribute_friendly_name_e_prescription_code
        HealthIdCredentialClaimDefinition.AFFILIATION_COUNTRY -> Res.string.attribute_friendly_name_affiliation_country
        HealthIdCredentialClaimDefinition.ISSUE_DATE -> Res.string.attribute_friendly_name_issue_date
        HealthIdCredentialClaimDefinition.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
        HealthIdCredentialClaimDefinition.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
        HealthIdCredentialClaimDefinition.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
        HealthIdCredentialClaimDefinition.ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
        HealthIdCredentialClaimDefinition.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
        HealthIdCredentialClaimDefinition.ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
    }
}

