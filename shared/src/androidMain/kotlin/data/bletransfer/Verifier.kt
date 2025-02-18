package data.bletransfer

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import data.bletransfer.util.RequestBluetoothPermissions
import data.bletransfer.verifier.Entry
import data.bletransfer.verifier.TransferManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

actual fun getVerifier(): Verifier {
    return AndroidVerifier()
}

class AndroidVerifier : Verifier {
    private val TAG: String = "AndroidVerifier"

    private val permissionState = MutableStateFlow(false)
    private val transferManagerState = MutableStateFlow<TransferManager?>(null)

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    override fun getRequirements() {
        RequestBluetoothPermissions { granted -> permissionState.value = granted }

        if (transferManagerState.value == null) {
            transferManagerState.value = TransferManager.getInstance(LocalContext.current)
        }
    }

    override fun verify(
        qrcode: String,
        requestedDocument: Verifier.Document,
        updateLogs: (String?, String) -> Unit,
        updateData: (List<Entry>) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            updateLogs(TAG, "Waiting for requirements to load")
            Napier.d(tag = TAG, message = "Waiting for Permissions")

            val transferManager = transferManagerState.filterNotNull().first()
            val permissionGranted = permissionState.first { it }

            if (permissionGranted) {
                updateLogs(TAG, "Requirements are loaded, and permissions granted")
                Napier.d(tag = TAG, message = "Permissions granted and TransferManager is ready")

                updateLogs(TAG, "Starting Device engagement with scanned Qr-code")

                transferManager.let {
                    it.setUpdateAndRequest(updateLogs, requestedDocument) { le: List<Entry> ->
                        updateData(le)
                        transferManager.closeConnection()
                    }
                    it.initVerificationHelper()
                    it.setQrDeviceEngagement(qrcode)
                }
            }
        }
    }

    override fun disconnect() {
        transferManagerState.value?.closeConnection()
    }
}
