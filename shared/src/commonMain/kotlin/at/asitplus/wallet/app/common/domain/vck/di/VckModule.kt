package at.asitplus.wallet.app.common.domain.vck.di

import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di.tokenStatusListModule
import at.asitplus.wallet.app.common.iso.verifier.ProximityResponseValidator
import at.asitplus.wallet.app.common.trust.RegistrationCertificateAnchors
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
import at.asitplus.wallet.lib.validation.StatusValidator
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ui.models.CredentialFreshnessSummaryModelEvaluator

fun vckModule() = module {
    scope(named(SESSION_NAME)) {
        scoped<StatusValidator> {
            StatusValidator(statusListTokenResolver = get<StatusListTokenResolver>())
        }
        scoped<CredentialValidator> {
            CredentialValidator(
                trustAnchors = get<TrustListService>().credentialTrustAnchors(),
                statusValidator = get(),
            )
        }
        scoped<RegistrationCertificateAnchors> {
            RegistrationCertificateAnchors(
                httpService = get(),
                dataStoreService = get(),
                settingsRepository = get(),
            )
        }
        scoped<RelyingPartyValidator> {
            val registrationCertificates = get<RegistrationCertificateAnchors>().trustedCertificates
            RelyingPartyValidator(
                trust = RelyingPartyTrustAnchors(
                    accessCertificates = get<TrustListService>().trustedCertificates(LoteProfile.WRPAC),
                    registrationCertificates = registrationCertificates,
                    registrationStatus = registrationCertificates,
                ),
                statusValidator = get(),
            )
        }
        scoped<IssuerMetadataValidator> {
            val registrationCertificates = get<RegistrationCertificateAnchors>().trustedCertificates
            IssuerMetadataValidator(
                accessCertificates = get<TrustListService>().trustedCertificates(LoteProfile.WRPAC),
                registrationCertificates = registrationCertificates,
                registrationStatus = registrationCertificates,
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
        scoped<CredentialFreshnessSummaryModelEvaluator> {
            val storedCredentialValidator = get<StoredCredentialValidator>()
            CredentialFreshnessSummaryModelEvaluator { storedCredentialValidator.freshness(it) }
        }
    }
    includes(tokenStatusListModule())
}
