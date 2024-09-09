package data.bletransfer.holder

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.android.identity.util.Constants
import com.android.identity.mdoc.request.DeviceRequestParser
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.OptionalLong

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
                val parser = DeviceRequestParser()
                parser.setSessionTranscript(presentation.sessionTranscript)
                parser.setDeviceRequest(requestBytes.value)
                return parser.parse()
            } ?: throw IllegalStateException("Presentation not set")
        } ?: throw IllegalStateException("Request not received")
    }

    fun getSessionTranscript(): ByteArray? {
        return deviceRetrievalHelper?.sessionTranscript
    }

    fun sendResponse(deviceResponse: ByteArray, closeAfterSending: Boolean) {
        val progressListener: (Long, Long) -> Unit = { progress, max ->
            Napier.d( tag = TAG, message = "Progress: $progress of $max")
            if (progress == max) {
                Napier.d( tag = TAG, message = "Completed...")
            }
        }
        if (closeAfterSending) {
            deviceRetrievalHelper?.sendDeviceResponse(
                deviceResponse,
                OptionalLong.of(Constants.SESSION_DATA_STATUS_SESSION_TERMINATION),
                progressListener,
                Dispatchers.IO.asExecutor()//context.mainExecutor()
            )
            deviceRetrievalHelper?.disconnect()
        } else {
            deviceRetrievalHelper?.sendDeviceResponse(
                deviceResponse,
                OptionalLong.empty(),
                progressListener,
                Dispatchers.IO.asExecutor()//context.mainExecutor()
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
                    null,
                    OptionalLong.of(Constants.SESSION_DATA_STATUS_SESSION_TERMINATION)
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