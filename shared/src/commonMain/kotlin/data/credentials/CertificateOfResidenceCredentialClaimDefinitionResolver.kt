package data.credentials

class CertificateOfResidenceCredentialClaimDefinitionResolver {
    fun resolveOrNull(claimReference: SingleClaimReference) = when (claimReference) {
        is MdocClaimReference -> CertificateOfResidenceCredentialMdocClaimDefinitionResolver().resolveOrNull(
            claimReference.namespace,
            claimReference.claimName,
        )

        is SdJwtClaimReference -> CertificateOfResidenceCredentialSdJwtClaimDefinitionResolver().resolveOrNull(
            claimReference.normalizedJsonPath
        )
    }
}