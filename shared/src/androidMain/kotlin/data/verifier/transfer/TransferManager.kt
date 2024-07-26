package ui.data.transfer


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.android.identity.android.mdoc.deviceretrieval.VerificationHelper
import com.android.identity.android.mdoc.transport.DataTransportOptions
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.request.DeviceRequestGenerator
import data.verifier.Entry
import data.verifier.Verifier
import data.verifier.transfer.util.CborDecoder
import ui.data.transfer.settings.UserPreferences
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.concurrent.Executor

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
    var updateLogs: (String) -> Unit = {}
    var requestedDocumentID: Verifier.Document? = null
    var updateData: (Entry) -> Unit = {}
    var sentRequest: Boolean = false

    private var mdocConnectionMethod: ConnectionMethod? = null

    private var hasStarted = false
    var responseBytes: ByteArray? = null
        private set
    private var verification: VerificationHelper? = null
    private var availableMdocConnectionMethods: Collection<ConnectionMethod>? = null

    private var transferStatusLd = MutableLiveData<TransferStatus>()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val userPreferences = UserPreferences(sharedPreferences)

    //fun getTransferStatus(): LiveData<TransferStatus> = transferStatusLd

    fun setUpdateAndRequest(updateLogs: (String) -> Unit, requestedDocumentID: Verifier.Document, updateData: (Entry) -> Unit) {
        this.updateLogs = updateLogs
        this.requestedDocumentID = requestedDocumentID
        this.updateData = updateData
        this.sentRequest = false
    }

    fun initVerificationHelper() {
        updateLogs("initializing Verification helper for Transfer manager")
        val builder = VerificationHelper.Builder(
            context,
            responseListener,
            context.mainExecutor()
        )
        val options = DataTransportOptions.Builder()
            .setBleUseL2CAP(userPreferences.isBleL2capEnabled())
            .setBleClearCache(userPreferences.isBleClearCacheEnabled())
            .build()
        builder.setDataTransportOptions(options)
        verification = builder.build()
    }

    fun setQrDeviceEngagement(qrDeviceEngagement: String) {
        Log.d(TAG, "setQrDeviceEngagement")
        updateLogs("Extracting bluetooth connection information from QR code")
        verification?.setDeviceEngagementFromQrCode(qrDeviceEngagement)
    }

    fun setAvailableTransferMethods(availableMdocConnectionMethods: Collection<ConnectionMethod>) {
        this.availableMdocConnectionMethods = availableMdocConnectionMethods
        if (availableMdocConnectionMethods.isNotEmpty()) {
            this.mdocConnectionMethod = availableMdocConnectionMethods.first()
        }
    }

    fun connect() {
        Log.d(TAG, "connect")
        updateLogs("Starting connection to Device")
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
            Log.e("TransferManager", "Error ignored. $e")
        }
        disconnect()
    }

    private fun disconnect(){
        try {
            verification?.disconnect()
        } catch (e: RuntimeException) {
            Log.e("TransferManager", "Error ignored. $e")
        }
        transferStatusLd = MutableLiveData<TransferStatus>()
        destroy()
        hasStarted = false
        updateLogs("Disconnected")
    }

    private fun destroy() {
        responseBytes = null
        verification = null
    }

    private val responseListener = object : VerificationHelper.Listener {
        override fun onReaderEngagementReady(readerEngagement: ByteArray) {
            this@TransferManager.readerEngagement = readerEngagement
            transferStatusLd.value = TransferStatus.READER_ENGAGEMENT_READY
        }

        override fun onDeviceEngagementReceived(connectionMethods: MutableList<ConnectionMethod>) {
            updateLogs("Ready for device engagement")

            setAvailableTransferMethods(ConnectionMethod.disambiguate(connectionMethods))
            transferStatusLd.value = TransferStatus.ENGAGED
            Log.d(TAG, "Device.ENGAGED")

            connect()
        }

        override fun onMoveIntoNfcField() {
            throw IllegalArgumentException()
        }

        override fun onDeviceConnected() {
            transferStatusLd.value = TransferStatus.CONNECTED
            Log.d(TAG, "Device.CONNECTED")
            if (!sentRequest) {
                sentRequest = true
                updateLogs("Connected to Device sending Request")
                sendRequest()
                updateLogs("Request has been sent and waiting for response")
            } else {
                // with IOS here multiple connections happen and no data is loaded
                // this prevents it but still has errors probably in the underlying library's
                updateLogs("Something went wrong. Device Connected again")
            }
        }

        override fun onResponseReceived(deviceResponseBytes: ByteArray) {
            responseBytes = deviceResponseBytes
            transferStatusLd.value = TransferStatus.RESPONSE
            Log.d(TAG, "Device.RESPONSE")
            updateLogs("Response received")

            verification?.let { v ->
                val cborfact = CborDecoder(updateLogs)
                cborfact.decode(deviceResponseBytes, v.sessionTranscript, v.ephemeralReaderKey)
                
                
                Log.d(TAG, "Session Transcript: " + v.sessionTranscript)
                Log.d(TAG, "Ephemeral Reader Key: " + v.ephemeralReaderKey)
                Log.d(TAG, "is Transport Specific Termination Supported: " + v.isTransportSpecificTerminationSupported)

                for (e in cborfact.entryList) {
                    updateData(e)
                }
            }?: throw IllegalStateException("Verification is null")
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            transferStatusLd.value = TransferStatus.DISCONNECTED
            Log.d(TAG, "Device.DISCONNECTED")
            updateLogs("Device disconnected")
            stopVerification(false, transportSpecificTermination)
        }

        override fun onError(error: Throwable) {
            Log.e("TransferManager", "onError: ${error.message}")
            updateLogs("Following error has occurred: ${error.message}")
            transferStatusLd.value = TransferStatus.ERROR
            stopVerification(
                sendSessionTerminationMessage = false,
                useTransportSpecificSessionTermination = false
            )
        }
    }

    private fun Context.mainExecutor(): Executor {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mainExecutor
        } else {
            ContextCompat.getMainExecutor(context)
        }
    }

    fun sendRequest() {
        if (verification == null)
            throw IllegalStateException("It is necessary to start a new engagement.")

        updateLogs("sending request for: $requestedDocumentID")

        verification?.let {
            var signature: Signature? = null
            var readerKeyCertificateChain: Collection<X509Certificate>? = null

            val generator =
                DeviceRequestGenerator()
            generator.setSessionTranscript(it.sessionTranscript)
            generator.addDocumentRequest(
                requestedDocumentID!!.docType,
                requestedDocumentID!!.requestDocument,
                null,
                signature,
                readerKeyCertificateChain
            )
            verification?.sendRequest(generator.generate())
        }
    }
}
