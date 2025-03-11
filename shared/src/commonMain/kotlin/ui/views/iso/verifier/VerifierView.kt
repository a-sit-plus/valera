package ui.views.iso.verifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import ui.viewmodels.iso.VerifierState
import ui.viewmodels.iso.VerifierViewModel

@Composable
fun VerifierView(
    vm: VerifierViewModel,
    bottomBar: @Composable () -> Unit
) {
    val vm = remember { vm }
    val verifierState by vm.verifierState.collectAsState()

    when (verifierState) {
        VerifierState.INIT -> {
            VerifierDocumentSelectionView(vm, bottomBar)
        }

        VerifierState.SELECT_CUSTOM_REQUEST -> {
            VerifierCustomSelectionView(vm)
        }

        VerifierState.QR_ENGAGEMENT -> {
            VerifierQrEngagementView(vm)
        }

        VerifierState.PRESENTATION -> TODO()
    }
}
