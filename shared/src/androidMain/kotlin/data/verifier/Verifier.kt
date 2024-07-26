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

    override fun verify(qrcode: String, requestedDocument: Verifier.Document, updateLogs: (String) -> Unit, updateData: (Entry) -> Unit) {

        val logoutput: (String) -> Unit = { s ->
            Log.d("DebugScreen", s)
            updateLogs(s)
        }
        CoroutineScope(Dispatchers.Main).launch {
            logoutput("Waiting for requirements to load")
            while (transferManager == null) {
                delay(500)
                Log.d("AndroidVerifier", "waiting for Transfer Manager")
            }
            logoutput("Waiting for ble and location permissions")
            while (!permission) {
                delay(500)
                Log.d("AndroidVerifier", "waiting for Permissions")
            }
            logoutput("Requirements are loaded and needed permissions given")

            Log.d("AndroidVerifier", "Transfer Manager is here")


            logoutput("Starting Device engagement with scanned Qr-code")

            val tM: TransferManager = transferManager!!
            tM.setUpdateAndRequest(logoutput, requestedDocument, updateData)
            tM.initVerificationHelper()
            tM.setQrDeviceEngagement(qrcode)
        }
    }

    override fun disconnect() {
        transferManager?.closeConnection()
    }

}
