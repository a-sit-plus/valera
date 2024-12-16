package ui.views.Authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ui.viewmodels.Authentication.AuthenticationConsentViewModel
import ui.viewmodels.Authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.Authentication.AuthenticationSelectionViewModel
import ui.viewmodels.Authentication.AuthenticationViewModel
import ui.viewmodels.Authentication.AuthenticationViewState

@Composable
fun AuthenticationView(vm: AuthenticationViewModel) {
    val vm = remember { vm }
    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    when (vm.viewState) {
        AuthenticationViewState.Consent -> {
            val viewModel = AuthenticationConsentViewModel(
                spName = vm.spName,
                spLocation = vm.spLocation,
                spImage = vm.spImage,
                requests = vm.parametersMap,
                navigateUp = vm.navigateUp,
                buttonConsent = { vm.onConsent() },
                walletMain = vm.walletMain
            )
            AuthenticationConsentView(viewModel)
        }

        AuthenticationViewState.NoMatchingCredential -> {
            val viewModel =
                AuthenticationNoCredentialViewModel(navigateToHomeScreen = vm.navigateToHomeScreen)
            AuthenticationNoCredentialView(vm = viewModel)
        }

        AuthenticationViewState.Selection -> {
            val viewModel = AuthenticationSelectionViewModel(walletMain = vm.walletMain,
                requests = vm.requestMap,
                confirmSelections = { selections ->
                    vm.confirmSelection(selections)
                }, navigateUp = { vm.viewState = AuthenticationViewState.Consent })
            AuthenticationSelectionView(vm = viewModel)
        }
    }
}
