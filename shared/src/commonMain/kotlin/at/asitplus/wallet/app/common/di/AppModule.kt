package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.CredentialValidityService
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.LoadingStatusService
import at.asitplus.wallet.app.common.RealCapabilitiesService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.di.dataModule
import at.asitplus.wallet.app.common.domain.di.domainModule
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import ui.navigation.IntentService

fun appModule(): Module = module {
    singleOf(::LocalPresentmentSessionCoordinator)

    scope(named(SESSION_NAME)) {
        scoped {
            WalletMain(
                keyMaterial = get(),
                dataStoreService = get(),
                platformAdapter = get(),
                subjectCredentialStore = get(),
                buildContext = get(),
                promptModel = get(),
                credentialValidator = get(),
                holderAgent = get(),
                provisioningService = get(),
                httpService = get(),
                presentationService = get(),
                signingService = get(),
                dcApiExportService = get(),
                errorService = get(),
                loadingStatusService = get(),
                snackbarService = get(),
                settingsRepository = get(),
                localPresentmentSessionCoordinator = get(),
                sessionService = get(),
                capabilitiesService = get(),
                credentialValidityService = get(),
                attestationService = get(),
                sessionCoroutineScope = get(),
                trustListService = get()
            )
        }
        scopedOf(::ErrorService)
        scopedOf(::LoadingStatusService)
        scopedOf(::CredentialValidityService)
        scopedOf(::TrustListService)
        scopedOf(::RealCapabilitiesService) binds arrayOf(CapabilitiesService::class)
        scopedOf(::IntentService)
    }

    includes(dataModule())
    includes(uiModule())
    includes(domainModule())
}
