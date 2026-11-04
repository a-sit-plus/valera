package at.asitplus.wallet.app.common.domain.communications.di

import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SigningService
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import at.asitplus.wallet.app.common.domain.vck.di.ISSUANCE
import at.asitplus.wallet.app.common.validation.PendingCredentialReview
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

fun communicationsModule() = module {
    singleOf(::HttpService)
    scope(named(SESSION_NAME)) {
        scoped {
            ProvisioningService(
                intentService = get(),
                dataStoreService = get(),
                keyMaterial = get(),
                keystoreService = get(),
                holderAgent = get(ISSUANCE),
                subjectCredentialStore = get(),
                config = get(),
                errorService = get(),
                httpService = get(),
                attestationService = get(),
                issuerMetadataValidator = get(),
                pendingCredentialReview = get(),
            )
        } onClose { it?.close() }
        scopedOf(::PresentationService) onClose { it?.close() }
        scopedOf(::SigningService)
        scopedOf(::DCAPIExportService)
        scopedOf(::AttestationService)
        scopedOf(::PendingCredentialReview) onClose { it?.clear() }
    }
}
