package ui.views.iso.verifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.viewmodels.iso.VerifierState
import ui.viewmodels.iso.VerifierViewModel

@Composable
fun VerifierView(
    vm: VerifierViewModel,
    bottomBar: @Composable () -> Unit
) {
    val vm = remember { vm }

    val verifierState by vm.verifierState.collectAsState()

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

        VerifierState.PRESENTATION -> TODO()
    }
}
