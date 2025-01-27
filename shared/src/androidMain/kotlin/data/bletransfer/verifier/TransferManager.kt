package data.bletransfer.verifier

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.android.identity.android.mdoc.deviceretrieval.VerificationHelper
import com.android.identity.android.mdoc.transport.DataTransportOptions
import com.android.identity.crypto.Algorithm
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.request.DeviceRequestGenerator
import data.bletransfer.Verifier
import data.bletransfer.util.CborDecoder
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

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

    private val TAG: String = "TransferManager"

    var readerEngagement: ByteArray? = null
    var updateLogs: (String?, String) -> Unit = { _: String?, _: String -> }
    var requestedDocumentID: Verifier.Document? = null
    var updateData: (List<Entry>) -> Unit = {}
    var sentRequest: Boolean = false

    private var mdocConnectionMethod: ConnectionMethod? = null
    private var hasStarted = false
    var responseBytes: ByteArray? = null
        private set
    private var verification: VerificationHelper? = null
    private var availableMdocConnectionMethods: Collection<ConnectionMethod>? = null

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun setUpdateAndRequest(updateLogs: (String?, String) -> Unit, requestedDocumentID: Verifier.Document, updateData: (List<Entry>) -> Unit) {
        this.updateLogs = updateLogs
        this.requestedDocumentID = requestedDocumentID
        this.updateData = updateData
    }

    fun initVerificationHelper() {
        updateLogs(TAG, "initializing Verification helper for Transfer manager")
        val builder = VerificationHelper.Builder(
            context,
            responseListener,
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
        updateLogs(TAG, "Extracting bluetooth connection information from QR code")
        verification?.setDeviceEngagementFromQrCode(qrDeviceEngagement)
    }

    fun setAvailableTransferMethods(availableMdocConnectionMethods: Collection<ConnectionMethod>) {
        this.availableMdocConnectionMethods = availableMdocConnectionMethods
        if (availableMdocConnectionMethods.isNotEmpty()) {
            this.mdocConnectionMethod = availableMdocConnectionMethods.first()
        }
    }

    fun connect() {
        updateLogs(TAG, "Starting connection to Device")
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
            Napier.e(tag = TAG, message =  "Error ignored. $e")
        }
        disconnect()
    }

    private fun disconnect(){
        try {
            verification?.disconnect()
        } catch (e: RuntimeException) {
            Napier.e(tag = TAG, message =  "Error ignored. $e")
        }
        destroy()
        hasStarted = false
        updateLogs(TAG, "Disconnected")
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
            updateLogs(TAG, "Ready for device engagement")
            setAvailableTransferMethods(ConnectionMethod.disambiguate(connectionMethods))

            connect()
        }

        override fun onMoveIntoNfcField() {
            throw IllegalArgumentException()
        }

        override fun onDeviceConnected() {
            if (!sentRequest) {
                sentRequest = true
                updateLogs(TAG, "Connected to Device sending Request")
                sendRequest()
                updateLogs(TAG, "Request has been sent and waiting for response")
            } else {
                // with IOS here multiple connections happen and no data is loaded
                // this prevents it but still has errors probably in the underlying library's
                updateLogs(TAG, "Something went wrong. Device Connected again")
            }
        }

        override fun onResponseReceived(deviceResponseBytes: ByteArray) {
            responseBytes = deviceResponseBytes
            updateLogs(TAG, "Response received")
            val sessionTranscript = verification?.sessionTranscript
            val ephemeralReaderKey = verification?.eReaderKey

            val cborDecoder = CborDecoder(updateLogs)
            cborDecoder.decodeResponse(deviceResponseBytes, sessionTranscript, ephemeralReaderKey)
            updateData(cborDecoder.entryList)
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            updateLogs(TAG, "Device disconnected")
            stopVerification(
                sendSessionTerminationMessage = false,
                useTransportSpecificSessionTermination = transportSpecificTermination
            )
        }

        override fun onError(error: Throwable) {
            updateLogs(TAG, "Following error has occurred: ${error.message}")
            stopVerification(
                sendSessionTerminationMessage = false,
                useTransportSpecificSessionTermination = false
            )
        }
    }

    fun sendRequest() {
        if (verification == null)
            throw IllegalStateException("It is necessary to start a new engagement.")

        updateLogs(TAG, "sending request for: $requestedDocumentID")

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
