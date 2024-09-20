package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.DOCUMENT_NUMBER
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.EXPIRY_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.E_SERVICE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.FULL_POWERS
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUANCE_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUING_AUTHORITY
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUING_COUNTRY
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUING_JURISDICTION
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.LEGAL_NAME
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_administrative_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_document_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_e_service
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_effective_from_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_effective_until_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_expiry_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_full_powers
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issue_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_authority
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_jurisdiction
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_legal_name
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_legal_person_identifier
import org.jetbrains.compose.resources.StringResource


object PowerOfRepresentationCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                LEGAL_PERSON_IDENTIFIER -> Res.string.attribute_friendly_name_legal_person_identifier
                LEGAL_NAME -> Res.string.attribute_friendly_name_legal_name
                FULL_POWERS -> Res.string.attribute_friendly_name_full_powers
                E_SERVICE -> Res.string.attribute_friendly_name_e_service
                EFFECTIVE_FROM_DATE -> Res.string.attribute_friendly_name_effective_from_date
                EFFECTIVE_UNTIL_DATE -> Res.string.attribute_friendly_name_effective_until_date
                ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
                ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                else -> null
            }

            else -> null
        }
}