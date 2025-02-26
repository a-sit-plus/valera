package ui.views.iso.datarequest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ui.viewmodels.iso.datarequest.DataRequestConsentViewModel
import ui.viewmodels.iso.datarequest.DataRequestViewModel
import ui.viewmodels.iso.datarequest.DataRequestViewState

const val TAG = "HandleRequestedDataView"

@Composable
fun DataRequestView(vm: DataRequestViewModel) {
    val vm = remember { vm }
    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    when (vm.viewState) {
        DataRequestViewState.Consent -> {
            val viewModel = DataRequestConsentViewModel(
                requestedAttributes = vm.attributes,
                navigateUp = vm.navigateUp,
                buttonConsent = { vm.onConsent() },
                walletMain = vm.walletMain
            )
            DataRequestConsentView(viewModel)
        }

        DataRequestViewState.Selection -> {
            DataRequestSelectionView(
                walletMain = vm.walletMain,
                changeToLoading = { vm.viewState = DataRequestViewState.Loading },
                changeToSent = { vm.viewState = DataRequestViewState.Sent }
            )
        }

        DataRequestViewState.Loading -> DataRequestSendingView()

        DataRequestViewState.Sent -> DataRequestSentView()

        DataRequestViewState.NoMatchingCredential -> TODO()
    }
}
