package at.asitplus.wallet.app.common.di

import at.asitplus.wallet.app.common.SnackbarService
import data.storage.CryptoKeyRepository
import data.storage.CryptoKeyRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.viewmodels.AddCredentialViewModel
import ui.viewmodels.CapabilitiesViewModel
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.InitializationViewModel
import ui.viewmodels.LoadWithFIIssuerViewModel
import ui.viewmodels.SettingsViewModel
import ui.viewmodels.SignatureRequestsViewModel
import ui.viewmodels.authentication.AuthenticationSuccessViewModel
import ui.viewmodels.iso.holder.HolderViewModel
import ui.viewmodels.iso.common.TransferOptionsViewModel
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.viewmodels.UserProfileViewModel
fun uiModule() = module {
    singleOf(::SnackbarService)
    single<CryptoKeyRepository> {
        CryptoKeyRepositoryImpl(get())
    }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::CredentialsViewModel)
    viewModelOf(::TransferOptionsViewModel)
    viewModelOf(::HolderViewModel)
    viewModelOf(::VerifierViewModel)
    viewModelOf(::AuthenticationSuccessViewModel)
    viewModelOf(::AddCredentialViewModel)
    viewModelOf(::CapabilitiesViewModel)
    viewModelOf(::InitializationViewModel)
    viewModelOf(::LoadWithFIIssuerViewModel)
    viewModelOf(::UserProfileViewModel)
    viewModelOf(::SignatureRequestsViewModel)
}
