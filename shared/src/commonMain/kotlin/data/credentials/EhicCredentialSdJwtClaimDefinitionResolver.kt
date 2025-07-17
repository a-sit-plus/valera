package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.ehic.EhicScheme

class EhicCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(path: NormalizedJsonPath) = when (val first = path.segments.firstOrNull()) {
        is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
            EhicScheme.Attributes.ISSUING_COUNTRY -> EhicCredentialClaimDefinition.ISSUING_COUNTRY
            EhicScheme.Attributes.SOCIAL_SECURITY_NUMBER -> EhicCredentialClaimDefinition.SOCIAL_SECURITY_NUMBER
            EhicScheme.Attributes.PERSONAL_ADMINISTRATIVE_NUMBER -> EhicCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER

            EhicScheme.Attributes.PREFIX_ISSUING_AUTHORITY -> when (val second = path.segments.drop(1).firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                    EhicScheme.Attributes.IssuingAuthority.ID -> EhicCredentialClaimDefinition.ISSUING_AUTHORITY_ID
                    EhicScheme.Attributes.IssuingAuthority.NAME -> EhicCredentialClaimDefinition.ISSUING_AUTHORITY_NAME
                    else -> null
                }


                null -> EhicCredentialClaimDefinition.ISSUING_AUTHORITY
                else -> null
            }

            EhicScheme.Attributes.PREFIX_AUTHENTIC_SOURCE -> when (val second = path.segments.drop(1).firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                    EhicScheme.Attributes.AuthenticSource.ID -> EhicCredentialClaimDefinition.AUTHENTIC_SOURCE_ID
                    EhicScheme.Attributes.AuthenticSource.NAME -> EhicCredentialClaimDefinition.AUTHENTIC_SOURCE_NAME
                    else -> null
                }

                null -> EhicCredentialClaimDefinition.AUTHENTIC_SOURCE
                else -> null
            }

            EhicScheme.Attributes.DOCUMENT_NUMBER -> EhicCredentialClaimDefinition.DOCUMENT_NUMBER
            EhicScheme.Attributes.ISSUANCE_DATE -> EhicCredentialClaimDefinition.ISSUANCE_DATE
            EhicScheme.Attributes.DATE_OF_ISSUANCE -> EhicCredentialClaimDefinition.DATE_OF_ISSUANCE
            EhicScheme.Attributes.EXPIRY_DATE -> EhicCredentialClaimDefinition.EXPIRY_DATE
            EhicScheme.Attributes.DATE_OF_EXPIRY -> EhicCredentialClaimDefinition.DATE_OF_EXPIRY
            EhicScheme.Attributes.STARTING_DATE -> EhicCredentialClaimDefinition.STARTING_DATE
            EhicScheme.Attributes.ENDING_DATE -> EhicCredentialClaimDefinition.ENDING_DATE


            else -> null
        }

        else -> null
    }
}