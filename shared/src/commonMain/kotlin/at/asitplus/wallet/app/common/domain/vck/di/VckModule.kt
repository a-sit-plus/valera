package at.asitplus.wallet.app.common.domain.vck.di

import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.DeterministicCoseHeaderVerifySignature
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di.tokenStatusListModule
import at.asitplus.wallet.app.common.iso.verifier.ProximityResponseValidator
import at.asitplus.wallet.app.common.validation.StoredCredentialValidator
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.etsi.LoteProfile
import at.asitplus.wallet.lib.validation.CredentialValidator
import at.asitplus.wallet.lib.validation.IssuerMetadataValidator
import at.asitplus.wallet.lib.validation.RelyingPartyTrustAnchors
import at.asitplus.wallet.lib.validation.RelyingPartyValidator
import at.asitplus.wallet.lib.validation.SignatureCheck
import at.asitplus.wallet.lib.validation.StatusValidator
import at.asitplus.wallet.lib.validation.TrustPurpose
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ui.models.CredentialFreshnessSummaryModelEvaluator

/**
 * Qualifies the validation of credentials received from a credential issuer, which checks their status with a status
 * list fetched again instead of a cached one, as the wallet stores them only after showing their validation.
 */
val ISSUANCE = named("issuance")

/**
 * Verifies the signatures of status list CWTs also if their protected header is not in the order Signum encodes it in,
 * see [DeterministicCoseHeaderVerifySignature].
 */
private fun statusValidator(statusListTokenResolver: StatusListTokenResolver) = StatusValidator(
    statusListTokenResolver = statusListTokenResolver,
    signatureCheck = SignatureCheck(DeterministicCoseHeaderVerifySignature()),
)

fun vckModule() = module {
    scope(named(SESSION_NAME)) {
        scoped<StatusValidator> {
            statusValidator(get<StatusListTokenResolver>())
        }
        scoped<CredentialValidator> {
            CredentialValidator(
                trustAnchors = get<TrustListService>().credentialTrustAnchors(),
                statusValidator = get(),
            )
        }
        scoped<CredentialValidator>(ISSUANCE) {
            CredentialValidator(
                trustAnchors = get<TrustListService>().credentialTrustAnchors(),
                statusValidator = statusValidator(get<StatusListTokenResolver>(ISSUANCE)),
            )
        }
        scoped<RelyingPartyValidator> {
            RelyingPartyValidator(
                trust = RelyingPartyTrustAnchors(
                    accessCertificates = get<TrustListService>().trustedCertificates(LoteProfile.WRPAC),
                    registrationCertificates = get<TrustListService>().trustedCertificates(LoteProfile.WRPRC),
                    registrationStatus = get<TrustListService>()
                        .trustedCertificates(LoteProfile.WRPRC, TrustPurpose.STATUS),
                ),
                statusValidator = get(),
            )
        }
        scoped<IssuerMetadataValidator> {
            IssuerMetadataValidator(
                accessCertificates = get<TrustListService>().trustedCertificates(LoteProfile.WRPAC),
                registrationCertificates = get<TrustListService>().trustedCertificates(LoteProfile.WRPRC),
                registrationStatus = get<TrustListService>()
                    .trustedCertificates(LoteProfile.WRPRC, TrustPurpose.STATUS),
                statusValidator = get(),
            )
        }
        scopedOf(::StoredCredentialValidator)
        scopedOf(::ProximityResponseValidator)
        scoped<HolderAgent> {
            val keyMaterial: KeyMaterial by inject()
            val subjectCredentialStore: SubjectCredentialStore by inject()
            HolderAgent(
                keyMaterial = keyMaterial,
                subjectCredentialStore = subjectCredentialStore,
                credentialValidator = get(),
            )
        }
        // Shares the key and the store of the default holder, and validates credentials as received, see ISSUANCE
        scoped<HolderAgent>(ISSUANCE) {
            HolderAgent(
                keyMaterial = get(),
                subjectCredentialStore = get(),
                credentialValidator = get(ISSUANCE),
            )
        }
        scoped<CredentialFreshnessSummaryModelEvaluator> {
            val storedCredentialValidator = get<StoredCredentialValidator>()
            CredentialFreshnessSummaryModelEvaluator { storedCredentialValidator.freshness(it) }
        }
    }
    includes(tokenStatusListModule())
}
