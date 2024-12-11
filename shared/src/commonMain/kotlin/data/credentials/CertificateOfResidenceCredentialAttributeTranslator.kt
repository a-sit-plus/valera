package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_arrival_date
import at.asitplus.valera.resources.attribute_friendly_name_birth_place
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_lastname
import at.asitplus.valera.resources.attribute_friendly_name_nationality
import at.asitplus.valera.resources.attribute_friendly_name_residence_address
import at.asitplus.valera.resources.attribute_friendly_name_sex
import org.jetbrains.compose.resources.StringResource


object CertificateOfResidenceCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        with(CertificateOfResidenceDataElements) {
            when (val first = attributeName.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                    ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
                    ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                    EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                    ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                    DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                    ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                    ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                    FAMILY_NAME -> Res.string.attribute_friendly_name_firstname
                    GIVEN_NAME -> Res.string.attribute_friendly_name_lastname
                    BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                    RESIDENCE_ADDRESS -> Res.string.attribute_friendly_name_residence_address
                    GENDER -> Res.string.attribute_friendly_name_sex
                    BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
                    ARRIVAL_DATE -> Res.string.attribute_friendly_name_arrival_date
                    NATIONALITY -> Res.string.attribute_friendly_name_nationality
                    else -> null
                }

                else -> null
            }
        }
}
