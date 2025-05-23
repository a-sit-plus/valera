package at.asitplus.wallet.app.common.data.di

import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.data.SettingsRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.authentication.AuthenticationQrCodeScannerViewModel

fun dataModule() = module {
    singleOf(::WalletConfig) {
        bind<SettingsRepository>()
    }
    singleOf(::CredentialsViewModel)
    singleOf(::AuthenticationQrCodeScannerViewModel)
}