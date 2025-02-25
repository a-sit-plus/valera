package ui.views.iso.datarequest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_select_requested_data
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.datarequest.DataRequestConsentViewModel
import ui.viewmodels.iso.datarequest.DataRequestViewModel
import ui.viewmodels.iso.datarequest.DataRequestViewState

const val TAG = "HandleRequestedDataView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRequestView(vm: DataRequestViewModel) {
    val vm = remember { vm }
    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    // TODO:
    //   - Remove the Scaffold for this class (only handle which view should be loaded)
    //   - Complete views for different cases

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_select_requested_data),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Logo()
                    }
                },
                navigationIcon = { NavigateUpButton(vm.navigateUp) }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
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

                DataRequestViewState.Loading -> { DataRequestSendingView() }

                DataRequestViewState.Sent -> { DataRequestSentView() }

                DataRequestViewState.NoMatchingCredential -> TODO()
            }
        }
    }
}
