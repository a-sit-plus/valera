package ui.views.iso.verifier

import androidx.compose.foundation.Image
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
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.iso.transfer.BluetoothInfo
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.composables.LabeledContent
import ui.composables.LabeledText
import ui.viewmodels.iso.VerifierState
import ui.viewmodels.iso.VerifierViewModel
import ui.views.ErrorView
import kotlin.math.min

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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    vm.deviceResponse.value!!.documents!!.forEach {
                        Text("DocType: ${it.docType}")
                        it.issuerSigned.namespaces?.forEach { namespace ->
                            namespace.value.entries.sortedBy { it.value.elementIdentifier }
                                .forEach { entry ->
                                    val label = NormalizedJsonPath(
                                        NormalizedJsonPathSegment.NameSegment(namespace.key),
                                        NormalizedJsonPathSegment.NameSegment(entry.value.elementIdentifier),
                                    ).toString()
                                    if (entry.value.elementIdentifier == "portrait") {
                                        LabeledContent(
                                            content = { Base64Image(entry.value.elementValue.toString()) },
                                            label = label
                                        )
                                    } else {
                                        LabeledText(
                                            text = entry.value.elementValue.prettyToString()
                                                .run { slice(0..min(lastIndex, 100)) },
                                            label = label
                                        )
                                    }
                                }
                        }
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
        }
    }
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}

@Composable
fun Base64Image(base64: String) {
    val dataUri = "data:image/png;base64,$base64"
    Image(
        painter = rememberAsyncImagePainter(model = dataUri),
        contentDescription = null,
        modifier = Modifier
    )
}
