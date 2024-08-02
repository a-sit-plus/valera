package data.holder.transfer

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.android.identity.android.legacy.*
import com.android.mdl.app.transfer.Communication
import com.android.mdl.app.transfer.QrCommunicationSetup

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



    fun startQrEngagement(updateQrCode: (String) -> Unit) {
        if (hasStarted) {
            throw IllegalStateException("Transfer has already started.")
        }
        communication = Communication.getInstance(context)
        qrCommunicationSetup = QrCommunicationSetup(
            context = context,
            onConnecting = {
                Log.d(TAG, "CONNECTING")
            },
            onQrEngagementReady = { qrText ->
                Log.d(TAG, "QrCode: $qrText")
                updateQrCode(qrText)
            },
            onDeviceRetrievalHelperReady = { session, deviceRetrievalHelper ->
                this.session = session
                communication.setupPresentation(deviceRetrievalHelper)
                Log.d(TAG, "CONNECTED")
            },
            onNewDeviceRequest = { deviceRequest ->
                communication.setDeviceRequest(deviceRequest)
                Log.d(TAG, "REQUEST received")
            },
            onDisconnected = {
                Log.d(TAG, "DISCONNECTED")
                stopPresentation(false, false)
            },
            onCommunicationError = { error ->
                Log.d(TAG, "onError: ${error.message}")
            }
        ).apply {
            configure()
        }
        hasStarted = true
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