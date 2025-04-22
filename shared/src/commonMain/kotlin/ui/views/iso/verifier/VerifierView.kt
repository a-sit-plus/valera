package ui.views.iso.verifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.iso.transfer.BluetoothInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.viewmodels.ErrorViewModel
import ui.viewmodels.iso.VerifierState
import ui.viewmodels.iso.VerifierViewModel
import ui.views.ErrorView

@Composable
fun VerifierView(
    vm: VerifierViewModel,
    bottomBar: @Composable () -> Unit
) {
    val vm = remember { vm }

    val verifierState by vm.verifierState.collectAsState()

    val isBluetoothEnabled = BluetoothInfo().isBluetoothEnabled()
    if (!isBluetoothEnabled) {
        // TODO: add string resource
        vm.handleError("Bluetooth is not available, please turn it on.")
    }

    val blePermissionState = rememberBluetoothPermissionState()
    if (!blePermissionState.isGranted) {
        CoroutineScope(Dispatchers.Main).launch {
            blePermissionState.launchPermissionRequest()
        }
    }

    when (verifierState) {
        VerifierState.INIT -> {
            VerifierDocumentSelectionView(vm, bottomBar)
        }

        VerifierState.SELECT_CUSTOM_REQUEST -> {
            VerifierCustomSelectionView(vm)
        }

        VerifierState.QR_ENGAGEMENT -> {
            //TODO handle rejected permission state
            VerifierQrEngagementView(vm)
        }

        VerifierState.WAITING_FOR_RESPONSE -> {
            // TODO: make view for that
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    // TODO: add string resource
                    Text("Waiting for response")
                }
            }
        }

        VerifierState.PRESENTATION -> {
            VerifierPresentationView(vm)
        }

        VerifierState.ERROR -> {
            ErrorView(
                ErrorViewModel(
                    resetStack = vm.navigateToHomeScreen,
                    message = vm.errorMessage.value,
                    cause = null,
                    onClickLogo = vm.onClickLogo
                )
            )
        }
    }
}
