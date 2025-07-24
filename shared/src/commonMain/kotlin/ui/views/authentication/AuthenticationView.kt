package ui.views.authentication

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.lib.openid.DCQLMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ui.models.toCredentialFreshnessSummaryModel
import ui.viewmodels.authentication.AuthenticationConsentViewModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.AuthenticationSelectionDCQLView
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel
import ui.viewmodels.authentication.AuthenticationViewModel
import ui.viewmodels.authentication.AuthenticationViewState

@Composable
fun AuthenticationView(
    vm: AuthenticationViewModel,
    onError: (Throwable) -> Unit,
) {
    vm.walletMain.keyMaterial.onUnauthenticated = vm.navigateUp
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
                onClickLogo = vm.onClickLogo,
                onClickSettings = vm.onClickSettings
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
            when (val matching = vm.matchingCredentials) {
                is DCQLMatchingResult -> {
                    AuthenticationSelectionDCQLView(
                        navigateUp = vm.navigateUp,
                        onClickLogo = vm.onClickLogo,
                        onClickSettings = vm.onClickSettings,
                        confirmSelection = { vm.confirmSelection(it) },
                        matchingResult = matching,
                        checkCredentialFreshness = {
                            vm.walletMain.checkCredentialFreshness(it).toCredentialFreshnessSummaryModel()
                        },
                        decodeToBitmap = { vm.walletMain.platformAdapter.decodeImage(it) },
                        onError = onError,
                    )
                }

                is PresentationExchangeMatchingResult -> {
                    AuthenticationSelectionPresentationExchangeView(
                        onClickLogo = vm.onClickLogo,
                        onClickSettings = vm.onClickSettings,
                        vm = AuthenticationSelectionPresentationExchangeViewModel(
                            walletMain = vm.walletMain,
                            confirmSelections = { selections -> vm.confirmSelection(selections) },
                            navigateUp = { vm.viewState = AuthenticationViewState.Consent },
                            credentialMatchingResult = matching,
                            navigateToHomeScreen = vm.navigateToHomeScreen
                        ),
                        onError = onError,
                    )
                }
            }
        }
    }
}