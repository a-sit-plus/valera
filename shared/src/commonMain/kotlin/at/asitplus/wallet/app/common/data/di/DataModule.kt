package at.asitplus.wallet.app.common.data.di

import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.data.SettingsRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun dataModule() = module {
    scope(named(SESSION_NAME)) {
        scopedOf(::WalletConfig) {
            bind<SettingsRepository>()
        }
    }
}
