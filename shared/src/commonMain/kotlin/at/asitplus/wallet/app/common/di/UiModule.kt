package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.SnackbarService
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.SettingsViewModel
import ui.viewmodels.authentication.AuthenticationSuccessViewModel

fun uiModule() = module {
    singleOf(::SnackbarService)

    // TODO: replace with viewModelOf as soon as we figure out how to set LocalViewModelStoreOwner in instrumented tests
    viewModelOf(::SettingsViewModel)
    viewModelOf(::CredentialsViewModel)
    viewModelOf(::AuthenticationSuccessViewModel)
}