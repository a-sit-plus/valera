package ui.views.authentication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialSetQuery
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_complex_dcql_query
import at.asitplus.valera.resources.error_unsatisfiable_dcql_query
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.composables.DCQLCredentialQuerySubmissionSelection
import ui.composables.DCQLCredentialQuerySubmissionSelectionOption
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.state.savers.rememberMutableStateListOf
import ui.viewmodels.authentication.AuthenticationConsentViewModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.AuthenticationSelectionDCQLView
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel
import ui.viewmodels.authentication.AuthenticationViewModel
import ui.viewmodels.authentication.AuthenticationViewState
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.DCQLMatchingResult
import ui.viewmodels.authentication.PresentationExchangeMatchingResult

@Composable
fun AuthenticationView(
    vm: AuthenticationViewModel,
    onError: (Throwable) -> Unit,
) {
    val vm = remember { vm }
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
            when (val matching = vm.matchingCredentials) {
                is DCQLMatchingResult -> {
                    AuthenticationSelectionDCQLView(
                        navigateUp = vm.navigateUp,
                        onClickLogo = vm.onClickLogo,
                        confirmSelection = {
                            vm.confirmSelection(it)
                        },
                        matchingResult = matching,
                        decodeToBitmap = { byteArray ->
                            vm.walletMain.platformAdapter.decodeImage(byteArray)
                        },
                        onError = onError,
                    )
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