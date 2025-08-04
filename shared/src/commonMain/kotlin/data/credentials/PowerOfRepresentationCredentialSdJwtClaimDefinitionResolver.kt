package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.por.PowerOfRepresentationDataElements

class PowerOfRepresentationCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(attributeName: NormalizedJsonPath): PowerOfRepresentationCredentialClaimDefinition? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER -> PowerOfRepresentationCredentialClaimDefinition.LEGAL_PERSON_IDENTIFIER
                PowerOfRepresentationDataElements.LEGAL_NAME -> PowerOfRepresentationCredentialClaimDefinition.LEGAL_NAME
                PowerOfRepresentationDataElements.FULL_POWERS -> PowerOfRepresentationCredentialClaimDefinition.FULL_POWERS
                PowerOfRepresentationDataElements.E_SERVICE -> PowerOfRepresentationCredentialClaimDefinition.E_SERVICE
                PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE -> PowerOfRepresentationCredentialClaimDefinition.EFFECTIVE_FROM_DATE
                PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE -> PowerOfRepresentationCredentialClaimDefinition.EFFECTIVE_UNTIL_DATE
                PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER -> PowerOfRepresentationCredentialClaimDefinition.ADMINISTRATIVE_NUMBER
                PowerOfRepresentationDataElements.ISSUANCE_DATE -> PowerOfRepresentationCredentialClaimDefinition.ISSUANCE_DATE
                PowerOfRepresentationDataElements.EXPIRY_DATE -> PowerOfRepresentationCredentialClaimDefinition.EXPIRY_DATE
                PowerOfRepresentationDataElements.ISSUING_AUTHORITY -> PowerOfRepresentationCredentialClaimDefinition.ISSUING_AUTHORITY
                PowerOfRepresentationDataElements.DOCUMENT_NUMBER -> PowerOfRepresentationCredentialClaimDefinition.DOCUMENT_NUMBER
                PowerOfRepresentationDataElements.ISSUING_COUNTRY -> PowerOfRepresentationCredentialClaimDefinition.ISSUING_COUNTRY
                PowerOfRepresentationDataElements.ISSUING_JURISDICTION -> PowerOfRepresentationCredentialClaimDefinition.ISSUING_JURISDICTION
                else -> null
            }

            else -> null
        }
}