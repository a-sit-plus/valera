package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.CredentialValidityService
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.RealCapabilitiesService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.di.dataModule
import at.asitplus.wallet.app.common.domain.di.domainModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import ui.navigation.IntentService

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
