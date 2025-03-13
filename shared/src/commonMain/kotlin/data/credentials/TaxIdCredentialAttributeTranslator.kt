package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
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
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.AFFILIATION_COUNTRY
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.BIRTH_DATE
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.CHURCH_TAX_ID
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.DOCUMENT_NUMBER
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.EXPIRY_DATE
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.IBAN
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUANCE_DATE
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUING_AUTHORITY
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUING_COUNTRY
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUING_JURISDICTION
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.PID_ID
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.REGISTERED_FAMILY_NAME
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.REGISTERED_GIVEN_NAME
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.RESIDENT_ADDRESS
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.TAX_NUMBER
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.VERIFICATION_STATUS
import org.jetbrains.compose.resources.StringResource


object TaxIdCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                TAX_NUMBER -> Res.string.attribute_friendly_name_tax_number
                AFFILIATION_COUNTRY -> Res.string.attribute_friendly_name_affiliation_country
                REGISTERED_FAMILY_NAME -> Res.string.attribute_friendly_name_registered_family_name
                REGISTERED_GIVEN_NAME -> Res.string.attribute_friendly_name_registered_given_name
                RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_resident_address
                BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                CHURCH_TAX_ID -> Res.string.attribute_friendly_name_church_tax_id
                IBAN -> Res.string.attribute_friendly_name_iban
                PID_ID -> Res.string.attribute_friendly_name_pid_id
                ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                VERIFICATION_STATUS -> Res.string.attribute_friendly_name_verification_status
                EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
                ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                else -> null
            }
            else -> null
        }
}