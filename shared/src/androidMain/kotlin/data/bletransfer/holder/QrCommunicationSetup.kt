package data.bletransfer.holder

import android.content.Context
import android.security.identity.PresentationSession
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.crypto.EcPrivateKey
import com.android.identity.crypto.EcPublicKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.security.PublicKey

class QrCommunicationSetup(
    private val context: Context,
    private val onConnecting: () -> Unit,
    private val onQrEngagementReady: (String) -> Unit,
    private val onDeviceRetrievalHelperReady: (session: PresentationSession, deviceRetrievalHelper: DeviceRetrievalHelper) -> Unit,
    private val onNewDeviceRequest: (request: ByteArray) -> Unit,
    private val onDisconnected: (transportSpecificTermination: Boolean) -> Unit,
    private val onCommunicationError: (error: Throwable) -> Unit,
) {
    private val TAG: String = "QrCommunicationSetup"

    private val session = SessionSetup(CredentialStore(context)).createSession()
    private val connectionSetup = ConnectionSetup(context)

    private val qrEngagementListener = object : QrEngagementHelper.Listener {

        fun onDeviceEngagementReady() {
            Napier.d( tag = TAG, message = "QR Engagement: Device Engagement Ready")
            onQrEngagementReady(deviceEngagementUriEncoded)
        }

        override fun onDeviceConnecting() {
            Napier.d(tag = TAG, message = "QR Engagement: Device Connecting")
            onConnecting()
        }

        override fun onDeviceConnected(transport: DataTransport) {
            if (deviceRetrievalHelper != null) {
                Napier.d(
                    tag = TAG,
                    message = "OnDeviceConnected for QR engagement -> ignoring due to active presentation"
                )
                return
            }
            Napier.d(tag = TAG, message = "OnDeviceConnected via QR: qrEngagement=$qrEngagement")
            val builder = DeviceRetrievalHelper.Builder(
                context,
                deviceRetrievalHelperListener,
                Dispatchers.IO.asExecutor(),//context.mainExecutor(),
                session.ephemeralKeyPair.private as EcPrivateKey
            )
            builder.useForwardEngagement(
                transport,
                qrEngagement.deviceEngagement,
                qrEngagement.handover
            )
            deviceRetrievalHelper = builder.build()
            qrEngagement.close()
            onDeviceRetrievalHelperReady(session, deviceRetrievalHelper!!)
        }

        override fun onError(error: Throwable) {
            Napier.d(tag = TAG, message = "QR onError: ${error.message}")
            onCommunicationError(error)
        }
    }

    private val deviceRetrievalHelperListener = object : DeviceRetrievalHelper.Listener {
        override fun onEReaderKeyReceived(eReaderKey: EcPublicKey) {
            Napier.d(
                tag = TAG,
                message = "DeviceRetrievalHelper Listener (QR): OnEReaderKeyReceived"
            )
            session.setSessionTranscript(deviceRetrievalHelper!!.sessionTranscript)
            session.setReaderEphemeralPublicKey(eReaderKey as PublicKey)
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

    val deviceEngagementUriEncoded: String
        get() = qrEngagement.deviceEngagementUriEncoded

    fun configure() {
        qrEngagement = QrEngagementHelper.Builder(
            context,
            session.ephemeralKeyPair.public as EcPublicKey,
            connectionSetup.getConnectionOptions(),
            qrEngagementListener,
            Dispatchers.IO.asExecutor()
        )
            .setConnectionMethods(connectionSetup.getConnectionMethods())
            .build()
    }

    fun close() {
        try {
            qrEngagement.close()
        } catch (exception: RuntimeException) {
            Napier.d(tag = TAG, message = "Error closing QR engagement $exception")
        }
    }
}
