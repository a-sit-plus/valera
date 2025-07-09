package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.SnackbarService
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.viewmodels.AddCredentialViewModel
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.SettingsViewModel
import ui.viewmodels.authentication.AuthenticationSuccessViewModel
import ui.viewmodels.iso.ShowQrCodeViewModel

fun uiModule() = module {
    singleOf(::SnackbarService)

    viewModelOf(::SettingsViewModel)
    viewModelOf(::CredentialsViewModel)
    viewModelOf(::ShowQrCodeViewModel)
    viewModelOf(::AuthenticationSuccessViewModel)
    viewModelOf(::AddCredentialViewModel)
}