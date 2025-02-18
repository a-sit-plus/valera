package data.bletransfer.holder

import android.annotation.SuppressLint
import android.content.Context
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
    private var hasStarted = false

    private lateinit var communication: Communication

    fun startQrEngagement(
        updateQrCode: (String) -> Unit,
        updateRequestedAttributes: (List<RequestedDocument>) -> Unit
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
                Napier.d(tag = TAG, message = "QrCode: $qrText")
                updateQrCode(qrText)
            },
            onDeviceRetrievalHelperReady = { deviceRetrievalHelper ->
                communication.setupPresentation(deviceRetrievalHelper)
                Napier.d(tag = TAG, message = "CONNECTED")
            },
            onNewDeviceRequest = { deviceRequest ->
                Napier.d(tag = TAG, message = "REQUEST received")
                communication.setDeviceRequest(deviceRequest)

                val cborDecoder = CborDecoder { tag, message ->
                    Napier.d( tag = tag, message = message)
                }

                cborDecoder.decodeRequest(deviceRequest)
                val documentRequestsList = cborDecoder.documentRequests

                if (documentRequestsList.isNotEmpty()) {
                    Napier.d(tag = TAG, message = "document requests:\n" +
                            documentRequestsList.joinToString("\n") { it.toString() }
                    )
                } else {
                    Napier.w(tag = TAG, message = "No document requests found in the request.")
                }

                updateRequestedAttributes(documentRequestsList)
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
