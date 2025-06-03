package ui.views.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.app.common.decodeImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ui.viewmodels.authentication.AuthenticationSelectionDCQLView
import ui.viewmodels.authentication.AuthenticationViewModel
import ui.viewmodels.authentication.AuthenticationViewState
import ui.viewmodels.authentication.DCQLMatchingResult
import ui.viewmodels.authentication.PresentationExchangeMatchingResult
import ui.viewmodels.authentication.PresentationExchangeSubmissionBuilder

@Composable
fun AuthenticationView(
    spName: String?,
    spLocation: String,
    spImage: ImageBitmap?,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: (redirectUrl: String?) -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    vm: AuthenticationViewModel,
    onError: (Throwable) -> Unit,
) {
    LaunchedEffect(Unit) {
        walletMain.keyMaterial.onUnauthenticated = navigateUp
    }

    when (vm.viewState) {
        AuthenticationViewState.Consent -> {
            AuthenticationConsentView(
                spName = spName,
                spLocation = spLocation,
                spImage = spImage,
                transactionData = vm.transactionData,
                navigateUp = navigateUp,
                consentToDataTransmission = {
                    CoroutineScope(Dispatchers.IO).launch {
                        vm.onConsent()
                    }
                },
                walletMain = walletMain,
                presentationRequest = vm.presentationRequest,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                onError = onError,
            )
        }

        AuthenticationViewState.NoMatchingCredential -> {
            AuthenticationNoCredentialView(
                navigateToHomeScreen = navigateToHomeScreen,
            )
        }

        AuthenticationViewState.Selection -> {
            when (val matching = vm.matchingCredentials) {
                is DCQLMatchingResult -> {
                    AuthenticationSelectionDCQLView(
                        navigateUp = navigateUp,
                        onClickLogo = onClickLogo,
                        onClickSettings = onClickSettings,
                        confirmSelection = {
                            vm.confirmSelection(it) {
                                navigateUp()
                                navigateToAuthenticationSuccessPage(it.redirectUri)
                            }
                        },
                        matchingResult = matching,
                        checkRevocationStatus = {
                            walletMain.checkRevocationStatus(it)
                        },
                        decodeToBitmap = { byteArray ->
                            walletMain.platformAdapter.decodeImage(byteArray)
                        },
                        onError = onError,
                    )
                }

                is PresentationExchangeMatchingResult -> {
                    AuthenticationSelectionPresentationExchangeView(
                        onClickLogo = onClickLogo,
                        onClickSettings = onClickSettings,
                        submissionBuilder = PresentationExchangeSubmissionBuilder(
                            credentialMatchingResult = matching,
                        ),
                        walletMain = walletMain,
                        navigateUp = { vm.viewState = AuthenticationViewState.Consent },
                        onError = onError,
                        onSubmit = { selections ->
                            vm.confirmSelection(selections) {
                                navigateUp()
                                navigateToAuthenticationSuccessPage(it.redirectUri)
                            }
                        }
                    )
                }
            }
        }
    }
}