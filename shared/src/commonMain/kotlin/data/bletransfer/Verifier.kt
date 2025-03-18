package data.bletransfer

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.transfer.TransferManager
import data.document.RequestDocument
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.permissions.RequestBluetoothPermissions

class Verifier(private val transferManager: TransferManager) {
    private val tag = "Verifier"
    private val permissionState = MutableStateFlow(false)

    @Composable
    fun getRequirements() {
        RequestBluetoothPermissions { granted -> permissionState.value = granted }
    }

    fun doVerifierFlow(
        qrcode: String,
        requestDocument: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        val permissionGranted = permissionState.first { it }

        if (permissionGranted) {
            if (qrcode.startsWith("mdoc:")) {
                transferManager.doQrFlow(qrcode.substring(5), requestDocument, setDeviceResponseBytes)
            } else {
                Napier.w("QR-Code does not start with \"mdoc:\"", tag = tag)
            }
        } else {
            Napier.w("Permissions not granted", tag = tag)
        }
    }

    fun startNfcEngagement(
        requestDocument: RequestDocument,
        updateProgress: (String) -> Unit,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        val permissionGranted = permissionState.first { it }

        if (permissionGranted) {
            transferManager.startNfcEngagement(requestDocument, setDeviceResponseBytes)
        } else {
            updateProgress("Permissions not granted, cannot proceed with verification")
            Napier.w("Permissions not granted")
        }
    }
}
