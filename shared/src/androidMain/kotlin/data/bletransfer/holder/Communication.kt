package data.bletransfer.holder

import android.annotation.SuppressLint
import android.content.Context
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.mdoc.request.DeviceRequestParser
import com.android.identity.util.Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
import io.github.aakira.napier.Napier

class Communication private constructor(
    private val context: Context,
) {

    private val TAG: String = "Communication"

    private var request: DeviceRequest? = null
    private var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    fun setupPresentation(deviceRetrievalHelper: DeviceRetrievalHelper) {
        this.deviceRetrievalHelper = deviceRetrievalHelper
    }

    fun setDeviceRequest(deviceRequest: ByteArray) {
        this.request = DeviceRequest(deviceRequest)
    }

    fun getDeviceRequest(): DeviceRequestParser.DeviceRequest {
        request?.let { requestBytes ->
            deviceRetrievalHelper?.let { presentation ->
                return DeviceRequestParser(
                    encodedDeviceRequest = requestBytes.value,
                    encodedSessionTranscript = presentation.sessionTranscript
                ).parse()
            } ?: throw IllegalStateException("Presentation not set")
        } ?: throw IllegalStateException("Request not received")
    }

    fun getSessionTranscript(): ByteArray? {
        return deviceRetrievalHelper?.sessionTranscript
    }

    fun sendResponse(deviceResponse: ByteArray, closeAfterSending: Boolean) {
//        val progressListener: (Long, Long) -> Unit = { progress, max ->
//            Napier.d( tag = TAG, message = "Progress: $progress of $max")
//            if (progress == max) {
//                Napier.d( tag = TAG, message = "Completed...")
//            }
//        }
        if (closeAfterSending) {
            deviceRetrievalHelper?.sendDeviceResponse(
                deviceResponseBytes = deviceResponse,
                status = SESSION_DATA_STATUS_SESSION_TERMINATION
            )
            deviceRetrievalHelper?.disconnect()
        } else {
            deviceRetrievalHelper?.sendDeviceResponse(
                deviceResponseBytes = deviceResponse,
                status = null
            )
        }
    }

    fun stopPresentation(
        sendSessionTerminationMessage: Boolean,
        useTransportSpecificSessionTermination: Boolean
    ) {
        if (sendSessionTerminationMessage) {
            if (useTransportSpecificSessionTermination) {
                deviceRetrievalHelper?.sendTransportSpecificTermination()
            } else {
                deviceRetrievalHelper?.sendDeviceResponse(
                    deviceResponseBytes = null,
                    status = SESSION_DATA_STATUS_SESSION_TERMINATION
                )
            }
        }
        disconnect()
    }

    fun disconnect() {
        request = null
        try {
            deviceRetrievalHelper?.disconnect()
        } catch (e: RuntimeException) {
            Napier.e(message = "Error ignored closing presentation $e")
        }
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Communication? = null

        fun getInstance(context: Context): Communication {
            return instance ?: synchronized(this) {
                instance ?: Communication(context).also { instance = it }
            }
        }
    }

    @JvmInline
    value class DeviceRequest(val value: ByteArray)
}
