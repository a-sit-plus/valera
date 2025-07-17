package data.credentials

import at.asitplus.wallet.ehic.EhicScheme

class EhicCredentialMdocClaimDefinitionResolver {
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): EhicCredentialClaimDefinition? = when (namespace) {
        EhicScheme.isoNamespace -> when (claimName) {
            EhicScheme.Attributes.ISSUING_COUNTRY -> EhicCredentialClaimDefinition.ISSUING_COUNTRY
            EhicScheme.Attributes.SOCIAL_SECURITY_NUMBER -> EhicCredentialClaimDefinition.SOCIAL_SECURITY_NUMBER
            EhicScheme.Attributes.PERSONAL_ADMINISTRATIVE_NUMBER -> EhicCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER
            EhicScheme.Attributes.PREFIX_ISSUING_AUTHORITY -> EhicCredentialClaimDefinition.ISSUING_AUTHORITY
            EhicScheme.Attributes.ISSUING_AUTHORITY_ID -> EhicCredentialClaimDefinition.ISSUING_AUTHORITY_ID
            EhicScheme.Attributes.ISSUING_AUTHORITY_NAME -> EhicCredentialClaimDefinition.ISSUING_AUTHORITY_NAME
            EhicScheme.Attributes.PREFIX_AUTHENTIC_SOURCE -> EhicCredentialClaimDefinition.AUTHENTIC_SOURCE
            EhicScheme.Attributes.AUTHENTIC_SOURCE_ID -> EhicCredentialClaimDefinition.AUTHENTIC_SOURCE_ID
            EhicScheme.Attributes.AUTHENTIC_SOURCE_NAME -> EhicCredentialClaimDefinition.AUTHENTIC_SOURCE_NAME
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

