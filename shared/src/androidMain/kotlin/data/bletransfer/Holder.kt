package data.bletransfer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import data.bletransfer.holder.TransferManager
import data.bletransfer.holder.PreferencesHelper
import data.bletransfer.verifier.ReceivedDocument
import data.bletransfer.util.RequestBluetoothPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


actual fun getHolder(): Holder = AndroidHolder()

class AndroidHolder: Holder {

    private var permission = false

    @Composable
    override fun getRequirements(check: (Boolean) -> Unit) {
        RequestBluetoothPermissions { b ->
            permission = b
            check(b)
        }
        PreferencesHelper.initialize(LocalContext.current)
        transferManager = TransferManager.getInstance(LocalContext.current)
    }
    private var transferManager: TransferManager? = null
    private val TAG: String = "AndroidHolder"

    override fun hold(updateQrCode: (String) -> Unit, updateRequestedAttributes: (List<ReceivedDocument>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            while (transferManager == null) {
                delay(500)
                Log.d(TAG, "waiting for Transfer Manager")
            }
            while (!permission) {
                delay(500)
                Log.d(TAG, "waiting for Permissions")
            }
            Log.d(TAG, "Transfer Manager is here")
            transferManager!!.startQrEngagement(updateQrCode, updateRequestedAttributes)
        }
    }

    override fun disconnect() {
        transferManager?.stopPresentation(true, false)
    }

}