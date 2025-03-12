package ui.views.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ui.viewmodels.authentication.AuthenticationConsentViewModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.AuthenticationSelectionViewModel
import ui.viewmodels.authentication.AuthenticationViewModel
import ui.viewmodels.authentication.AuthenticationViewState

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
                navigateUp = vm.navigateUp,
                buttonConsent = { vm.onConsent() },
                walletMain = vm.walletMain,
                transactionData = vm.transactionData,
                requests = vm.descriptors.toList(),
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
