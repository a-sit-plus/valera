package ui.views.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
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
                buttonConsent = {
                    CoroutineScope(Dispatchers.IO).launch {
                        vm.onConsent()
                    }
                },
                walletMain = vm.walletMain,
                transactionData = vm.transactionData,
                requests = vm.descriptors.toList(),
                presentationRequest = vm.presentationRequest,
                onClickLogo = vm.onClickLogo
            )
            AuthenticationConsentView(viewModel)
        }

        AuthenticationViewState.NoMatchingCredential -> {
            val viewModel =
                AuthenticationNoCredentialViewModel(navigateToHomeScreen = vm.navigateToHomeScreen)
            AuthenticationNoCredentialView(vm = viewModel)
        }

        AuthenticationViewState.Selection -> {
            val viewModel = AuthenticationSelectionViewModel(
                walletMain = vm.walletMain,
                requests = vm.requestMap,
                confirmSelections = { selections ->
                    vm.confirmSelection(selections)
                },
                navigateUp = { vm.viewState = AuthenticationViewState.Consent },
                onClickLogo = vm.onClickLogo,
            )
            AuthenticationSelectionView(vm = viewModel)
        }
    }
}
