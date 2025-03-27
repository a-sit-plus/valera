package ui.views.authentication

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ui.viewmodels.authentication.AuthenticationConsentViewModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel
import ui.viewmodels.authentication.AuthenticationViewModel
import ui.viewmodels.authentication.AuthenticationViewState
import ui.viewmodels.authentication.DCQLMatchingResult
import ui.viewmodels.authentication.PresentationExchangeMatchingResult

@Composable
fun AuthenticationView(
    vm: AuthenticationViewModel,
    onError: (Throwable) -> Unit,
) {
    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp
    when (vm.viewState) {
        AuthenticationViewState.Consent -> {
            val viewModel = AuthenticationConsentViewModel(
                spName = vm.spName,
                spLocation = vm.spLocation,
                spImage = vm.spImage,
                transactionData = vm.transactionData,
                navigateUp = vm.navigateUp,
                buttonConsent = {
                    CoroutineScope(Dispatchers.IO).launch {
                        vm.onConsent()
                    }
                },
                walletMain = vm.walletMain,
                presentationRequest = vm.presentationRequest,
                onClickLogo = vm.onClickLogo
            )
            AuthenticationConsentView(
                viewModel,
                onError = onError,
            )
        }

        AuthenticationViewState.NoMatchingCredential -> {
            val viewModel =
                AuthenticationNoCredentialViewModel(navigateToHomeScreen = vm.navigateToHomeScreen)
            AuthenticationNoCredentialView(vm = viewModel)
        }

        AuthenticationViewState.Selection -> {
            when(val matching = vm.matchingCredentials) {
                is DCQLMatchingResult -> {
                    AuthenticationSelectionViewScaffold(
                        onNavigateUp = vm.navigateUp,
                        onClickLogo = {},
                        onNext = {
                            vm.confirmSelection(null)
                        },
                    ) {
                        Column {
                            Text("Implementation of DCQL Query Credential Selection is in progress.")
                            Text("Click continue to submit the credentials that are selected by default.")
                        }
                    }
                }

                is PresentationExchangeMatchingResult -> {
                    val viewModel = AuthenticationSelectionPresentationExchangeViewModel(
                        walletMain = vm.walletMain,
                        confirmSelections = { selections ->
                            vm.confirmSelection(selections)
                        },
                        navigateUp = { vm.viewState = AuthenticationViewState.Consent },
                        onClickLogo = vm.onClickLogo,
                        credentialMatchingResult = matching,
                    )
                    AuthenticationSelectionPresentationExchangeView(vm = viewModel)
                }
            }
        }
    }
}
