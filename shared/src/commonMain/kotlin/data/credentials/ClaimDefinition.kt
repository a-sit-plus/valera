package data.credentials

import kotlin.jvm.JvmInline

sealed interface ClaimDefinition {
    @JvmInline
    value class CompanyRegistrationCredentialClaim(
        val value: CompanyRegistrationCredentialClaimDefinition
    ) : ClaimDefinition

    @JvmInline
    value class CertificateOfResidenceCredentialClaim(
        val value: CertificateOfResidenceCredentialClaimDefinition
    ) : ClaimDefinition
}