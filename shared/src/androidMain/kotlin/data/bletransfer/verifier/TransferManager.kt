package data.bletransfer.verifier

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import com.android.identity.android.mdoc.deviceretrieval.VerificationHelper
import com.android.identity.android.mdoc.transport.DataTransportOptions
import com.android.identity.crypto.Algorithm
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.request.DeviceRequestGenerator
import data.bletransfer.util.CborDecoder
import data.bletransfer.util.Document
import data.bletransfer.util.Entry
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class TransferManager private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: TransferManager? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: TransferManager(context).also { instance = it }
        }
    }

    private val TAG: String = "TransferManager"

    var readerEngagement: ByteArray? = null
    var requestedDocumentID: Document? = null
    var sentRequest: Boolean = false
    var updateData: (List<Entry>) -> Unit = {}

    private var mdocConnectionMethod: ConnectionMethod? = null
    private var hasStarted = false
    var responseBytes: ByteArray? = null
        private set
    private var verification: VerificationHelper? = null
    private var availableMdocConnectionMethods: Collection<ConnectionMethod>? = null

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun setUpdateAndRequest(requestedDocumentID: Document, updateData: (List<Entry>) -> Unit) {
        this.requestedDocumentID = requestedDocumentID
        this.updateData = updateData
    }

    fun initVerificationHelper() {
        Napier.d(tag = TAG, message = "Initializing VerificationHelper for TransferManager")
        val builder = VerificationHelper.Builder(
            context, responseListener,
            // This is important since with the main executor it would block the UI
            Dispatchers.IO.asExecutor()
        )
        val options = DataTransportOptions.Builder()
            .setBleUseL2CAP(sharedPreferences.getBoolean("ble_l2cap", false))
            .setBleClearCache(sharedPreferences.getBoolean("ble_clear_cache", false))
            .build()
        builder.setDataTransportOptions(options)
        verification = builder.build()
    }

    fun setQrDeviceEngagement(qrDeviceEngagement: String) {
        Napier.d(tag = TAG, message = "Extracting bluetooth connection information from QR code")
        verification?.setDeviceEngagementFromQrCode(qrDeviceEngagement)
    }

    fun setAvailableTransferMethods(availableMdocConnectionMethods: Collection<ConnectionMethod>) {
        this.availableMdocConnectionMethods = availableMdocConnectionMethods
        if (availableMdocConnectionMethods.isNotEmpty()) {
            this.mdocConnectionMethod = availableMdocConnectionMethods.first()
        }
    }

    fun connect() {
        Napier.d(tag = TAG, message = "Starting connection to Device")
        if (hasStarted)
            throw IllegalStateException("Connection has already started. It is necessary to stop verification before starting a new one.")

        if (verification == null)
            throw IllegalStateException("It is necessary to start a new engagement.")

        if (mdocConnectionMethod == null)
            throw IllegalStateException("No mdoc connection method selected.")

        // Start connection
        verification?.let {
            mdocConnectionMethod?.let { dr ->
                it.connect(dr)
            }
            hasStarted = true
        }
    }

    fun closeConnection() {
        if (hasStarted) {
            stopVerification(
                sendSessionTerminationMessage = true,
                useTransportSpecificSessionTermination = false
            )
        }
    }

    fun stopVerification(
        sendSessionTerminationMessage: Boolean,
        useTransportSpecificSessionTermination: Boolean
    ) {
        verification?.setSendSessionTerminationMessage(sendSessionTerminationMessage)
        try {
            if (verification?.isTransportSpecificTerminationSupported == true && useTransportSpecificSessionTermination) {
                verification?.setUseTransportSpecificSessionTermination(true)
            }
        } catch (e: IllegalStateException) {
            Napier.e(tag = TAG, message = "Error ignored. $e")
        }
        disconnect()
    }

    private fun disconnect() {
        try {
            verification?.disconnect()
        } catch (e: RuntimeException) {
            Napier.e(tag = TAG, message = "Error ignored. $e")
        }
        destroy()
        hasStarted = false
        Napier.d(tag = TAG, message = "Disconnected")
    }

    private fun destroy() {
        responseBytes = null
        verification = null
        sentRequest = false
    }

    private val responseListener = object : VerificationHelper.Listener {
        override fun onReaderEngagementReady(readerEngagement: ByteArray) {
            this@TransferManager.readerEngagement = readerEngagement
        }

        override fun onDeviceEngagementReceived(connectionMethods: List<ConnectionMethod>) {
            Napier.d(tag = TAG, message = "Device engagement received")
            setAvailableTransferMethods(ConnectionMethod.disambiguate(connectionMethods))
            connect()
        }

        override fun onMoveIntoNfcField() {
            throw IllegalArgumentException()
        }

        override fun onDeviceConnected() {
            if (!sentRequest) {
                sentRequest = true
                Napier.d(tag = TAG, message = "Connected to Device, sending Request")
                sendRequest()
            } else {
                // with IOS here multiple connections happen and no data is loaded
                // this prevents it but still has errors probably in the underlying library's
                Napier.w(tag = TAG, message = "Something went wrong. Device Connected again")
            }
        }

        override fun onResponseReceived(deviceResponseBytes: ByteArray) {
            Napier.d(tag = TAG, message = "Response received")
            responseBytes = deviceResponseBytes
            val sessionTranscript = verification?.sessionTranscript
            val ephemeralReaderKey = verification?.eReaderKey

            updateData(CborDecoder().apply {
                decodeResponse(deviceResponseBytes, sessionTranscript, ephemeralReaderKey)
            }.entryList)
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            Napier.d(tag = TAG, message = "Device disconnected")
            stopVerification(
                sendSessionTerminationMessage = false,
                useTransportSpecificSessionTermination = transportSpecificTermination
            )
        }

        override fun onError(error: Throwable) {
            Napier.e(tag = TAG, message = "Error: ${error.message}")
            stopVerification(
                sendSessionTerminationMessage = false,
                useTransportSpecificSessionTermination = false
            )
        }
    }

    fun sendRequest() {
        if (verification == null)
            throw IllegalStateException("It is necessary to start a new engagement.")

        Napier.d(tag = TAG, message = "Send request")
        verification?.let {
            val generator = DeviceRequestGenerator(it.sessionTranscript)
            generator.addDocumentRequest(
                docType = requestedDocumentID!!.docType,
                itemsToRequest = requestedDocumentID!!.requestDocument,
                requestInfo = null,
                readerKey = null,
                signatureAlgorithm = Algorithm.UNSET,
                readerKeyCertificateChain = null
            )
            verification?.sendRequest(generator.generate())
        }
    }
}
