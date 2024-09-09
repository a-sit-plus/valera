package data.bletransfer.holder

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.android.identity.android.legacy.*
import data.bletransfer.util.CborDecoder
import io.github.aakira.napier.Napier

class TransferManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TransferManager"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: TransferManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: TransferManager(context).also { instance = it }
            }
    }

    private var qrCommunicationSetup: QrCommunicationSetup? = null
    private var session: PresentationSession? = null
    private var hasStarted = false

    private lateinit var communication: Communication



    fun startQrEngagement(updateQrCode: (String) -> Unit, updateRequestedAttributes: (List<RequestedDocument>) -> Unit) {
        if (hasStarted) {
            throw IllegalStateException("Transfer has already started.")
        }
        communication = Communication.getInstance(context)
        qrCommunicationSetup = QrCommunicationSetup(
            context = context,
            onConnecting = {
                Napier.d( tag = TAG, message = "CONNECTING")
            },
            onQrEngagementReady = { qrText ->
                Napier.d( tag = TAG, message = "QrCode: $qrText")
                updateQrCode(qrText)
            },
            onDeviceRetrievalHelperReady = { session, deviceRetrievalHelper ->
                this.session = session
                communication.setupPresentation(deviceRetrievalHelper)
                Napier.d( tag = TAG, message = "CONNECTED")
            },
            onNewDeviceRequest = { deviceRequest ->
                communication.setDeviceRequest(deviceRequest)

                val cbordec = CborDecoder { tag, message -> Napier.d( tag = TAG, message = message) }
                cbordec.decodeRequest(deviceRequest)
                updateRequestedAttributes(cbordec.documentRequests)
                Napier.d( tag = TAG, message = "REQUEST received")

                val doc = cbordec.documentRequests
                for (i in doc) {
                    i.log()
                }
            },
            onDisconnected = {
                Napier.d( tag = TAG, message = "DISCONNECTED")
                stopPresentation(false, false)
            },
            onCommunicationError = { error ->
                Napier.d( tag = TAG, message = "onError: ${error.message}")
            }
        ).apply {
            configure()
        }
        hasStarted = true
    }

    fun sendResponse(deviceResponse: ByteArray, closeAfterSending: Boolean) {
        Napier.d( tag = TAG, message = "sendResponse")
        communication.sendResponse(deviceResponse, closeAfterSending)
    }

    fun stopPresentation(
        sendSessionTerminationMessage: Boolean,
        useTransportSpecificSessionTermination: Boolean
    ) {
        communication.stopPresentation(
            sendSessionTerminationMessage,
            useTransportSpecificSessionTermination
        )
        disconnect()
    }

    fun disconnect() {
        communication.disconnect()
        qrCommunicationSetup?.close()
        destroy()
    }

    fun destroy() {
        qrCommunicationSetup = null
        session = null
        hasStarted = false
    }
}