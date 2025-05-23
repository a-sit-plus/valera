package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.di.dataModule
import at.asitplus.wallet.app.common.domain.di.domainModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ui.navigation.IntentService

fun appModule(appDependencyProvider: WalletDependencyProvider) = module {
    singleOf(::WalletMain)
    singleOf(::ErrorService)
    singleOf(::IntentService)
    includes(dataModule())
    includes(uiModule())
    includes(domainModule(appDependencyProvider))
}


