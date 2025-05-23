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
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_one_time_token
import at.asitplus.valera.resources.attribute_friendly_name_patient_id
import at.asitplus.valera.resources.attribute_friendly_name_tax_number
import at.asitplus.wallet.healthid.HealthIdScheme.Attributes
import org.jetbrains.compose.resources.StringResource


object HealthIdCredentialAttributeTranslator : CredentialAttributeTranslator {

    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        attributeName.segments.firstOrNull()?.memberName()?.let { getFromIsoName(it) }
            ?: attributeName.segments.lastOrNull()?.memberName()?.let { getFromIsoName(it) }

    private fun NormalizedJsonPathSegment.memberName() = when (this) {
        is NormalizedJsonPathSegment.NameSegment -> this.memberName
        else -> null
    }

    private fun getFromIsoName(claimName: String): StringResource? = with(Attributes) {
        when (claimName) {
            HEALTH_INSURANCE_ID -> Res.string.attribute_friendly_name_health_insurance_id
            PATIENT_ID -> Res.string.attribute_friendly_name_patient_id
            TAX_NUMBER -> Res.string.attribute_friendly_name_tax_number
            ONE_TIME_TOKEN -> Res.string.attribute_friendly_name_one_time_token
            E_PRESCRIPTION_CODE -> Res.string.attribute_friendly_name_e_prescription_code
            AFFILIATION_COUNTRY -> Res.string.attribute_friendly_name_affiliation_country
            ISSUE_DATE -> Res.string.attribute_friendly_name_issue_date
            EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
            ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
            DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
            ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
            ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
            ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
            else -> null
        }
    }
}