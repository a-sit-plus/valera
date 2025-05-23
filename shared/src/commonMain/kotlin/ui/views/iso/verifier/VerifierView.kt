package ui.views.iso.verifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_waiting_for_response
import at.asitplus.wallet.app.common.iso.transfer.BluetoothInfo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.viewmodels.iso.VerifierState
import ui.viewmodels.iso.VerifierViewModel
import ui.views.LoadingView

@Composable
fun VerifierView(
    onError: (Throwable) -> Unit,
    bottomBar: @Composable () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    navigateUp: () -> Unit,
    vm: VerifierViewModel = koinViewModel(),
) {
    val verifierState by vm.verifierState.collectAsState()

    val isBluetoothEnabled = BluetoothInfo().isBluetoothEnabled()
    if (!isBluetoothEnabled) {
        // TODO if bluetooth is not available, NFC data transfer should still work
        //vm.handleError(stringResource(Res.string.error_bluetooth_unavailable))
    }

    val blePermissionState = rememberBluetoothPermissionState()
    if (!blePermissionState.isGranted) {
        vm.walletMain.scope.launch {
            blePermissionState.launchPermissionRequest()
        }
    }


    // TODO: from acrusage: this seems like a good candidate to use the navigation framework?
    when (verifierState) {
        VerifierState.INIT -> VerifierDocumentSelectionView(
            vm,
            bottomBar,
            onClickSettings = onClickSettings,
            onClickLogo = onClickLogo,
            navigateToCustomSelectionView = vm::navigateToCustomSelectionView,
        )
        VerifierState.SELECT_CUSTOM_REQUEST -> VerifierCustomSelectionView(
            vm,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            navigateUp = vm::navigateToVerifyDataView,
        )
        VerifierState.QR_ENGAGEMENT -> VerifierQrEngagementView(
            navigateUp = navigateUp,
            onClickLogo = onClickLogo,
            onFoundPayload = vm::onFoundPayload,
        )
        VerifierState.WAITING_FOR_RESPONSE -> LoadingView(
            stringResource(Res.string.info_text_waiting_for_response),
            navigateUp = navigateUp,
        )

        VerifierState.PRESENTATION -> VerifierPresentationView(
            navigateUp = navigateUp,
            onClickLogo = onClickLogo,
            vm,
        )
        VerifierState.ERROR -> onError(Throwable(vm.errorMessage.value))
    }
}
