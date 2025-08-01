package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.*
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.ID
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.NAME
import org.jetbrains.compose.resources.StringResource

@Suppress("DEPRECATION")
object EhicCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        with(EhicScheme.Attributes) {
            when (val first = attributeName.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                    ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                    SOCIAL_SECURITY_NUMBER -> Res.string.attribute_friendly_name_social_security_number
                    PERSONAL_ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_personal_administrative_number
                    ISSUING_AUTHORITY_ID -> Res.string.attribute_friendly_name_issuing_authority_id
                    ISSUING_AUTHORITY_NAME -> Res.string.attribute_friendly_name_issuing_authority_name
                    PREFIX_ISSUING_AUTHORITY -> when (val second =
                        attributeName.segments.drop(1).firstOrNull()) {
                        is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                            ID -> Res.string.attribute_friendly_name_issuing_authority_id
                            NAME -> Res.string.attribute_friendly_name_issuing_authority_name
                            else -> null
                        }

                        else -> Res.string.attribute_friendly_name_issuing_authority
                    }
                    AUTHENTIC_SOURCE_ID -> Res.string.attribute_friendly_name_authentic_source_id
                    AUTHENTIC_SOURCE_NAME -> Res.string.attribute_friendly_name_authentic_source_name
                    PREFIX_AUTHENTIC_SOURCE -> when (val second =
                        attributeName.segments.drop(1).firstOrNull()) {
                        is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                            ID -> Res.string.attribute_friendly_name_authentic_source_id
                            NAME -> Res.string.attribute_friendly_name_authentic_source_name
                            else -> null
                        }

                        else -> Res.string.attribute_friendly_name_authentic_source
                    }
                    DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                    ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                    DATE_OF_ISSUANCE -> Res.string.attribute_friendly_name_issue_date
                    EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                    DATE_OF_EXPIRY -> Res.string.attribute_friendly_name_expiry_date
                    STARTING_DATE -> Res.string.attribute_friendly_name_effective_from_date
                    ENDING_DATE -> Res.string.attribute_friendly_name_effective_until_date
                    else -> null
                }

                else -> null
            }
        }
}