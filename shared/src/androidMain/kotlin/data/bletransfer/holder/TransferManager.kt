package data.bletransfer.holder

import android.annotation.SuppressLint
import android.content.Context
import com.android.identity.cbor.Cbor
import data.bletransfer.util.RequestedDocument
import data.bletransfer.util.CborDecoder
import data.storage.CertificateStorage
import io.github.aakira.napier.Napier
import java.security.cert.X509Certificate

class TransferManager private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: TransferManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: TransferManager(context).also { instance = it }
            }
    }

    private val TAG = "TransferManager"

    private var qrCommunicationSetup: QrCommunicationSetup? = null
    private var hasStarted = false

    private lateinit var communication: Communication

    fun startQrEngagement(
        updateQrCode: (String) -> Unit,
        updateRequestedAttributes: (List<RequestedDocument>, String?) -> Unit
    ) {
        if (hasStarted) {
            throw IllegalStateException("Transfer has already started.")
        }
        communication = Communication.getInstance(context)
        qrCommunicationSetup = QrCommunicationSetup(
            context = context,
            onConnecting = {
                Napier.d(tag = TAG, message = "CONNECTING")
            },
            onQrEngagementReady = { qrText ->
                Napier.d(tag = TAG, message = "QR Engagement ready")
                updateQrCode(qrText)
            },
            onDeviceRetrievalHelperReady = { deviceRetrievalHelper ->
                communication.setupPresentation(deviceRetrievalHelper)
                Napier.d(tag = TAG, message = "CONNECTED")
            },
            onNewDeviceRequest = { deviceRequest ->
                Napier.d(tag = TAG, message = "REQUEST received")
                communication.setDeviceRequest(deviceRequest)
                val cbor = CborDecoder()
                val documentRequestsList = cbor.apply {
                    decodeRequest(deviceRequest, communication.getSessionTranscript(), context)
                }.documentRequests


                updateRequestedAttributes(documentRequestsList,
                    cbor.requesterIdentity
                )
            },
            onDisconnected = {
                Napier.d(tag = TAG, message = "DISCONNECTED")
                stopPresentation(
                    sendSessionTerminationMessage = false,
                    useTransportSpecificSessionTermination = false
                )
            },
            onCommunicationError = { error ->
                Napier.d(tag = TAG, message = "onError: ${error.message}")
            }
        ).apply {
            configure()
        }
        hasStarted = true
    }

    fun sendResponse(deviceResponse: ByteArray, closeAfterSending: Boolean) {
        Napier.d(tag = TAG, message = "sendResponse")
        communication.sendResponse(deviceResponse, closeAfterSending)
    }

    fun stopPresentation(
        sendSessionTerminationMessage: Boolean,
        useTransportSpecificSessionTermination: Boolean
    ) {
        Napier.d(tag = TAG, message = "stopPresentation")
        communication.stopPresentation(
            sendSessionTerminationMessage,
            useTransportSpecificSessionTermination
        )
        disconnect()
    }

    private fun disconnect() {
        Napier.d(tag = TAG, message = "disconnect")
        communication.disconnect()
        qrCommunicationSetup?.close()
        qrCommunicationSetup = null
        hasStarted = false
    }
}
