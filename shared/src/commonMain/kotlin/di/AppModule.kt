package di

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.data.SettingsRepository
import org.koin.dsl.module

fun appModule(walletMain: WalletMain) = module {
    // TODO: properly use dependency injection by not relying on WalletMain to do everything, simple for now for team evaluation
    single<SettingsRepository> {
        walletMain.walletConfig
    }
}