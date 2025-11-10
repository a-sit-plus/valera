package data.credentials

import at.asitplus.wallet.ageverification.AgeVerificationScheme

class AgeVerificationCredentialMdocClaimDefinitionResolver {
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): AgeVerificationCredentialClaimDefinition? = with(AgeVerificationScheme.Attributes) {
        when (namespace) {
            AgeVerificationScheme.isoNamespace -> when (claimName) {
                AGE_OVER_12 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_12
                AGE_OVER_13 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_13
                AGE_OVER_14 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_14
                AGE_OVER_16 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_16
                AGE_OVER_18 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_18
                AGE_OVER_21 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_21
                AGE_OVER_25 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_25
                AGE_OVER_60 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_60
                AGE_OVER_62 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_62
                AGE_OVER_65 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_65
                AGE_OVER_68 -> AgeVerificationCredentialClaimDefinition.AGE_OVER_68
                else -> null
            }

            else -> null
        }
    }
}