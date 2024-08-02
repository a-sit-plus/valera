package data.bletransfer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import data.bletransfer.verifier.Entry
import io.github.aakira.napier.Napier
import data.bletransfer.util.RequestBluetoothPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import data.bletransfer.verifier.TransferManager

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
                Napier.d(tag = TAG, message = "waiting for Transfer Manager")
            }
            while (!permission) {
                delay(500)
                Napier.d(tag = TAG, message = "waiting for Permissions")
            }
            updateLogs(TAG, "Requirements are loaded and needed permissions given")


            updateLogs(TAG, "Starting Device engagement with scanned Qr-code")

            transferManager?.let {
                it.setUpdateAndRequest(updateLogs, requestedDocument) { le: List<Entry> ->
                    updateData(le)
                    transferManager?.closeConnection()
                }
                it.initVerificationHelper()
                it.setQrDeviceEngagement(qrcode)
            } ?: Napier.d(tag = TAG, message = "The transferManager was set to null which should not have happened")
        }
    }

    override fun disconnect() {
        transferManager?.closeConnection()
    }

}
