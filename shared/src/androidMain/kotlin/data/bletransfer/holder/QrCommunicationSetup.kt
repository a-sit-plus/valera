package data.bletransfer.holder

import android.content.Context
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcCurve
import com.android.identity.crypto.EcPublicKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class QrCommunicationSetup(
    private val context: Context,
    private val onConnecting: () -> Unit,
    private val onQrEngagementReady: (String) -> Unit,
    private val onDeviceRetrievalHelperReady: (deviceRetrievalHelper: DeviceRetrievalHelper) -> Unit,
    private val onNewDeviceRequest: (request: ByteArray) -> Unit,
    private val onDisconnected: (transportSpecificTermination: Boolean) -> Unit,
    private val onCommunicationError: (error: Throwable) -> Unit,
) {
    private val TAG: String = "QrCommunicationSetup"

    private val eDeviceKey by lazy { Crypto.createEcPrivateKey(EcCurve.P256) }

    private val connectionSetup = ConnectionSetup(context)

    private val qrEngagementListener = object : QrEngagementHelper.Listener {
        override fun onDeviceConnecting() {
            Napier.d(tag = TAG, message = "QR Engagement: Device Connecting")
            onConnecting()
        }

        override fun onDeviceConnected(transport: DataTransport) {
            if (deviceRetrievalHelper != null) {
                Napier.d(tag = TAG, message = "OnDeviceConnected for QR engagement -> ignoring due to active presentation")
                return
            }

            Napier.d(tag = TAG, message = "OnDeviceConnected via QR: qrEngagement=$qrEngagement")
            val builder = DeviceRetrievalHelper.Builder(
                context,
                deviceRetrievalHelperListener,
                Dispatchers.IO.asExecutor(),
                eDeviceKey
            )

            builder.useForwardEngagement(
                transport,
                qrEngagement.deviceEngagement,
                qrEngagement.handover
            )

            deviceRetrievalHelper = builder.build()
            qrEngagement.close()
            onDeviceRetrievalHelperReady(deviceRetrievalHelper!!)
        }

        override fun onError(error: Throwable) {
            Napier.d(tag = TAG, message = "QR onError: ${error.message}")
            onCommunicationError(error)
        }
    }

    private val deviceRetrievalHelperListener = object : DeviceRetrievalHelper.Listener {
        override fun onEReaderKeyReceived(eReaderKey: EcPublicKey) {
            Napier.d(tag = TAG, message = "DeviceRetrievalHelper Listener (QR): OnEReaderKeyReceived")
        }

        override fun onDeviceRequest(deviceRequestBytes: ByteArray) {
            Napier.d(tag = TAG, message = "DeviceRetrievalHelper Listener (QR): OnDeviceRequest")
            onNewDeviceRequest(deviceRequestBytes)
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            Napier.d(tag = TAG, message = "DeviceRetrievalHelper Listener (QR): onDeviceDisconnected")
            onDisconnected(transportSpecificTermination)
        }

        override fun onError(error: Throwable) {
            Napier.d(tag = TAG, message = "DeviceRetrievalHelper Listener (QR): onError -> ${error.message}")
            onCommunicationError(error)
        }
    }

    private lateinit var qrEngagement: QrEngagementHelper
    private var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    fun configure() {
        qrEngagement = QrEngagementHelper.Builder(
            context,
            eDeviceKey.publicKey,
            connectionSetup.getConnectionOptions(),
            qrEngagementListener,
            Dispatchers.IO.asExecutor()
        )
            .setConnectionMethods(connectionSetup.getConnectionMethods())
            .build()

        Napier.d(tag = TAG, message = "configure: Device Engagement Ready")
        onQrEngagementReady(qrEngagement.deviceEngagementUriEncoded)
    }

    fun close() {
        Napier.d(tag = TAG, message = "closing QR engagement ...")
        try {
            qrEngagement.close()
        } catch (exception: RuntimeException) {
            Napier.d(tag = TAG, message = "Error closing QR engagement $exception")
        }
    }
}
