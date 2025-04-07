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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
        }

        VerifierState.PRESENTATION -> {
            // TODO: presentation view of response
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    vm.deviceResponse.value!!.documents!!.forEach {
                        Text("DocType: ${it.docType}")
                        it.issuerSigned.namespaces?.entries?.forEach {
                            Text("Namespace: ${it.key}")
                            it.value.entries.forEach {
                                val identifier = it.value.elementIdentifier
                                Text("Identifier: $identifier")
                                if (identifier == "portrait") {
                                    TODO()
                                } else {
                                    Text("Value: ${it.value.elementValue}")
                                }
                            }
                        }
                        Text("Errors: ${it.errors?.entries}")
                    }
                }
            }
        }

        VerifierState.ERROR -> {
            ErrorView(
                message = vm.errorMessage.value,
                resetStack = vm.navigateUp,
                cause = null,
                onClickLogo = vm.onClickLogo
            )
            // TODO: decide if ErrorView should be used for that
            //  - see ShowQrCodeView for comparison
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(vm.errorMessage.value)
//                }
//            }
        }
    }
}
