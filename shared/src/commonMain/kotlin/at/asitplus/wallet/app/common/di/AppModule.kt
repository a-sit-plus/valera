package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.di.dataModule
import at.asitplus.wallet.app.common.domain.di.domainModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ui.navigation.IntentService

fun appModule(appDependencyProvider: WalletDependencyProvider, capabilitiesModule: Module) = module {
    scope(named(SESSION_NAME)) {
        scopedOf(::WalletMain)
        scopedOf(::ErrorService)
    }
    includes(capabilitiesModule)

    single<KeystoreService>{
        appDependencyProvider.keystoreService
    }

    singleOf(::IntentService)
    singleOf(::SessionService)

    includes(dataModule())
    includes(uiModule())
    includes(domainModule(appDependencyProvider))
}


