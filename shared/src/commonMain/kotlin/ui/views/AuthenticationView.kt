package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ui.viewmodels.AuthenticationAttributesSelectionViewModel
import ui.viewmodels.AuthenticationConsentViewModel
import ui.viewmodels.AuthenticationCredentialSelectionViewModel
import ui.viewmodels.AuthenticationNoCredentialViewModel
import ui.viewmodels.AuthenticationViewModel
import ui.viewmodels.AuthenticationViewState

@Composable
fun AuthenticationView(vm: AuthenticationViewModel) {
    val vm = remember { vm }
    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    when(vm.viewState){
        AuthenticationViewState.Consent -> {
            val viewModel = AuthenticationConsentViewModel(spName = vm.spName,
                spLocation = vm.spLocation,
                spImage = vm.spImage,
                requests = vm.parametersMap,
                navigateUp = vm.navigateUp,
                buttonConsent = { vm.onConsent() },
                walletMain = vm.walletMain)
            AuthenticationConsentView(viewModel)
        }
        AuthenticationViewState.CredentialSelection -> {
            val viewModel = AuthenticationCredentialSelectionViewModel(walletMain = vm.walletMain,
                requests = vm.requestMap,
                selectCredential = { credential ->
                    vm.selectCredential(credential)
                }, navigateUp = { vm.viewState = AuthenticationViewState.Consent })
            AuthenticationCredentialSelectionView(viewModel)
        }
        AuthenticationViewState.AttributesSelection -> {
            val vm = AuthenticationAttributesSelectionViewModel(navigateUp = {vm.viewState = AuthenticationViewState.CredentialSelection},
                requests = vm.requestMap ,
                selectedCredentials = vm.selectedCredentials,
                selectAttributes = {attributes -> vm.selectAttributes(selectedAttributes = attributes)})
            AuthenticationAttributesSelectionView(vm)
        }
        AuthenticationViewState.NoMatchingCredential -> {
            val vm = AuthenticationNoCredentialViewModel(navigateToHomeScreen = vm.navigateToHomeScreen)
            AuthenticationNoCredentialView(vm = vm)
        }
    }
}
