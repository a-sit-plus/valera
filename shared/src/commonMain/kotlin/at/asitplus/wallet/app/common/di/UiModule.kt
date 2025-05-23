package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.SnackbarService
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.viewmodels.CredentialDetailsViewModel
import ui.viewmodels.SettingsViewModel
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.authentication.AuthenticationQrCodeScannerViewModel
import ui.viewmodels.iso.ShowQrCodeViewModel
import ui.viewmodels.iso.VerifierViewModel
import ui.views.OnboardingViewModel

fun uiModule() = module {
    singleOf(::SnackbarService)

    viewModelOf(::SettingsViewModel)
    viewModelOf(::CredentialsViewModel)
    viewModelOf(::AuthenticationQrCodeScannerViewModel)
    viewModelOf(::ShowQrCodeViewModel)
    viewModelOf(::VerifierViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::CredentialDetailsViewModel)
}