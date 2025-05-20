package di

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.data.SettingsRepository
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.viewmodels.SettingsViewModel

fun appModule(walletMain: WalletMain) = module {
    // TODO: properly use dependency injection by not relying on WalletMain to initialize everything
    single<SettingsRepository> {
        walletMain.walletConfig
    }
    viewModelOf(::SettingsViewModel)
}