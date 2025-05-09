package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.*
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.id
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.name
import org.jetbrains.compose.resources.StringResource

object EhicCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        with(EhicScheme.Attributes) {
            when (val first = attributeName.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                    ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                    SOCIAL_SECURITY_NUMBER -> Res.string.attribute_friendly_name_social_security_number
                    PREFIX_ISSUING_AUTHORITY -> when (val second = attributeName.segments.drop(1).firstOrNull()) {
                        is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                            id -> Res.string.attribute_friendly_name_issuing_authority_id
                            name -> Res.string.attribute_friendly_name_issuing_authority_name
                            else -> null
                        }
                        else -> Res.string.attribute_friendly_name_issuing_authority
                    }

                    ISSUING_AUTHORITY_ID -> Res.string.attribute_friendly_name_issuing_authority_id
                    ISSUING_AUTHORITY_NAME -> Res.string.attribute_friendly_name_issuing_authority_name
                    DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                    ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                    EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                    else -> null
                }

                else -> null
            }
        }
}