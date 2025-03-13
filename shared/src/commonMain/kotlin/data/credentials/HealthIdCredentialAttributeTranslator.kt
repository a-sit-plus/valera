package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_affiliation_country
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_e_prescription_code
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_health_insurance_id
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_one_time_token
import at.asitplus.valera.resources.attribute_friendly_name_patient_id
import at.asitplus.valera.resources.attribute_friendly_name_tax_number
import at.asitplus.wallet.healthid.HealthIdScheme.Attributes
import org.jetbrains.compose.resources.StringResource


object HealthIdCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                Attributes.HEALTH_INSURANCE_ID -> Res.string.attribute_friendly_name_health_insurance_id
                Attributes.PATIENT_ID -> Res.string.attribute_friendly_name_patient_id
                Attributes.TAX_NUMBER -> Res.string.attribute_friendly_name_tax_number
                Attributes.ONE_TIME_TOKEN -> Res.string.attribute_friendly_name_one_time_token
                Attributes.E_PRESCRIPTION_CODE -> Res.string.attribute_friendly_name_e_prescription_code
                Attributes.AFFILIATION_COUNTRY -> Res.string.attribute_friendly_name_affiliation_country
                Attributes.ISSUE_DATE -> Res.string.attribute_friendly_name_issue_date
                Attributes.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                Attributes.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                Attributes.ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
                Attributes.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                Attributes.ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                else -> null
            }

            else -> null
        }
}