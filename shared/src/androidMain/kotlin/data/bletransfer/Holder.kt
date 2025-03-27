package data.bletransfer

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.lib.iso.Document
import com.android.identity.mdoc.response.DeviceResponseGenerator
import com.android.identity.util.Constants.DEVICE_RESPONSE_STATUS_OK
import data.bletransfer.holder.TransferManager
import data.bletransfer.holder.PreferencesHelper
import data.bletransfer.util.RequestedDocument
import data.bletransfer.util.RequestBluetoothPermissions
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual fun getHolder(): Holder = AndroidHolder()

class AndroidHolder: Holder {
    private val TAG: String = "AndroidHolder"

    private var permission = false
    private var transferManager: TransferManager? = null
    private var requestedAttributes: List<RequestedDocument>? = null
    private var requesterIdentity: Map<String, String> = emptyMap()
    private var requesterVerified: Boolean = false

    @Composable
    override fun getRequirements(check: (Boolean) -> Unit) {
        RequestBluetoothPermissions { b ->
            permission = b
            check(b)
        }
        PreferencesHelper.initialize(LocalContext.current)
        // The following 2 lines are needed so that it works for the eAusweise app
        // (If omitted it works with the google verifier app)
        PreferencesHelper.setBleDataRetrievalEnabled(false)
        PreferencesHelper.setBlePeripheralDataRetrievalMode(true)

        transferManager = TransferManager.getInstance(LocalContext.current)
    }

    override fun getAttributes(): List<RequestedDocument> {
        return requestedAttributes ?: listOf()
    }

    override fun getRequesterIdentity(): Map<String, String> {
        return requesterIdentity
    }

    override fun getRequesterVerified(): Boolean {
        return requesterVerified
    }

    override fun hold(updateQrCode: (String) -> Unit, onRequestedAttributes: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            while (transferManager == null) {
                delay(500)
                Napier.d(tag = TAG, message ="waiting for Transfer Manager")
            }

            while (!permission) {
                delay(500)
                Napier.d(tag = TAG, message ="waiting for Permissions")
            }

            Napier.d(tag = TAG, message ="Transfer Manager is here")
            transferManager?.startQrEngagement(updateQrCode) {rA, identity, verified ->
                requestedAttributes = rA
                requesterIdentity = identity
                requesterVerified = verified
                onRequestedAttributes()
            } ?: Napier.d(tag = TAG, message = "The transferManager was set to null which should not have happened")
        }
    }

    override fun disconnect() {
        transferManager?.stopPresentation(
            sendSessionTerminationMessage = true,
            useTransportSpecificSessionTermination = false
        )
    }

    override fun send(credentials: List<Document>, launchAfterSuccessfulSend: () -> Unit) {
        val responseGenerator = DeviceResponseGenerator(DEVICE_RESPONSE_STATUS_OK)

        credentials.forEach { cred ->
            responseGenerator.addDocument(cred.serialize())
        }
        transferManager?.sendResponse(
            deviceResponse = responseGenerator.generate(),
            closeAfterSending = false
        )?: Napier.d(tag = TAG, message = "The transferManager was set to null which should not have happened")
        launchAfterSuccessfulSend()
    }
}
