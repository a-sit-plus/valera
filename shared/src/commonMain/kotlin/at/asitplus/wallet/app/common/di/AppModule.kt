package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.CredentialValidityService
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.RealCapabilitiesService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.WalletSessionBindings
import at.asitplus.wallet.app.common.data.di.dataModule
import at.asitplus.wallet.app.common.domain.di.domainModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.navigation.IntentService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun appModule(): Module = module {
    scope(named(SESSION_NAME)) {
        scopedOf(::WalletMain)
        scopedOf(::ErrorService)
        scopedOf(::CredentialValidityService)
        scopedOf(::RealCapabilitiesService) binds arrayOf(CapabilitiesService::class)
        scopedOf(::IntentService)
    }

    includes(dataModule())
    includes(uiModule())
    includes(domainModule())
}

@OptIn(ExperimentalUuidApi::class)
fun appModule(
    appDependencyProvider: WalletDependencyProvider,
    capabilitiesModule: Module
): Module = module {
    includes(appModule())
    includes(capabilitiesModule)

    single {
        SessionService().apply {
            initialize {
                val scope = KoinPlatform.getKoin().createScope(
                    "legacy-session:${Uuid.random()}",
                    named(SESSION_NAME)
                )
                scope.declare(
                    WalletSessionBindings(
                        intentState = at.asitplus.wallet.app.common.IntentState(),
                        sessionService = this,
                        buildContext = appDependencyProvider.buildContext,
                        promptModel = appDependencyProvider.promptModel,
                        platformAdapter = appDependencyProvider.platformAdapter,
                        dataStoreService = appDependencyProvider.dataStoreService,
                        keystoreService = appDependencyProvider.keystoreService
                    )
                )
                scope
            }
        }
    }
}
