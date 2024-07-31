package data.verifier

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import data.verifier.transfer.RequestBluetoothPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.data.transfer.TransferManager

actual fun getVerifier(): Verifier {
    return AndroidVerifier()
}

class AndroidVerifier: Verifier {
    private var permission = false

    @Composable
    override fun getRequirements(check: (Boolean) -> Unit) {
        RequestBluetoothPermissions { b ->
            permission = b
            check(b)
        }
        transferManager = TransferManager.getInstance(LocalContext.current)
    }
    private var transferManager: TransferManager? = null
    private val TAG: String = "AndroidVerifier"

    override fun verify(
        qrcode: String,
        requestedDocument: Verifier.Document,
        updateLogs: (String?, String) -> Unit,
        updateData: (List<Entry>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            updateLogs(TAG, "Waiting for requirements to load")
            while (transferManager == null) {
                delay(500)
                Log.d(TAG, "waiting for Transfer Manager")
            }
            updateLogs(TAG, "Waiting for ble and location permissions")
            while (!permission) {
                delay(500)
                Log.d(TAG, "waiting for Permissions")
            }
            updateLogs(TAG, "Requirements are loaded and needed permissions given")
            Log.d(TAG, "Transfer Manager is here")


            updateLogs(TAG, "Starting Device engagement with scanned Qr-code")

            val tM: TransferManager = transferManager!!
            tM.setUpdateAndRequest(updateLogs, requestedDocument) { le: List<Entry> ->
                updateData(le)
                transferManager?.closeConnection()
            }
            tM.initVerificationHelper()
            tM.setQrDeviceEngagement(qrcode)
        }
    }

    override fun disconnect() {
        transferManager?.closeConnection()
    }

}
