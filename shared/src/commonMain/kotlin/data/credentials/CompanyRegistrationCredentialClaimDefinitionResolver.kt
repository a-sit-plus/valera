package data.credentials

class CompanyRegistrationCredentialClaimDefinitionResolver {
    fun resolveOrNull(claimReference: SingleClaimReference) = when (claimReference) {
        is MdocClaimReference -> CompanyRegistrationCredentialMdocClaimDefinitionResolver().resolveOrNull(
            claimReference.namespace,
            claimReference.claimName,
        )

        is SdJwtClaimReference -> CompanyRegistrationCredentialSdJwtClaimDefinitionResolver().resolveOrNull(claimReference.normalizedJsonPath)
    }
}