package data.bletransfer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.lib.iso.Document
import com.android.identity.mdoc.response.DeviceResponseGenerator
import com.android.identity.util.Constants.DEVICE_RESPONSE_STATUS_OK
import data.bletransfer.holder.TransferManager
import data.bletransfer.holder.PreferencesHelper
import data.bletransfer.holder.RequestedDocument
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

    private var requestedAttributes: List<RequestedDocument>? = null

    override fun getAttributes(): List<RequestedDocument> {
        return requestedAttributes ?: listOf()
    }

    override fun hold(updateQrCode: (String) -> Unit, onRequestedAttributes: () -> Unit) {
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
            transferManager!!.startQrEngagement(updateQrCode) {rA ->
                requestedAttributes = rA
                onRequestedAttributes()
            }
        }
    }

    override fun disconnect() {
        transferManager?.stopPresentation(true, false)
    }

    override fun send(credentials: List<Document>, function: () -> Unit) {
        Log.d("myTag", credentials.toString())

        val responseGenerator = DeviceResponseGenerator(DEVICE_RESPONSE_STATUS_OK)

        credentials.forEach { cred ->
            responseGenerator.addDocument(cred.serialize())
        }
        transferManager!!.sendResponse(
            responseGenerator.generate(),
            false
        )
        function()
    }
}