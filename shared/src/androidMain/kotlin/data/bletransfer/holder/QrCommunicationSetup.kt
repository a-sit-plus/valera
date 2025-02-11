package data.bletransfer.holder

import android.content.Context
import com.android.identity.android.legacy.PresentationSession
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.crypto.EcCurve
import com.android.identity.crypto.EcPublicKey
import com.android.identity.crypto.javaPublicKey
import com.android.identity.crypto.toEcPrivateKey
import com.android.identity.crypto.toEcPublicKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

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

    private val session: PresentationSession

    init {
        val credentialStore = CredentialStore(context)
        session = SessionSetup(credentialStore).createSession()
    }

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
                // TODO: solve the casting problem
                //  java.lang.ClassCastException: com.android.org.conscrypt.OpenSSLECPrivateKey cannot be cast to org.bouncycastle.jce.interfaces.ECPrivateKey
                session.ephemeralKeyPair.private.toEcPrivateKey(session.ephemeralKeyPair.public, EcCurve.P256)
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
            Napier.d(tag = TAG, message = "DeviceRetrievalHelper Listener (QR): OnEReaderKeyReceived")
            session.setSessionTranscript(deviceRetrievalHelper!!.sessionTranscript)
            session.setReaderEphemeralPublicKey(eReaderKey.javaPublicKey)
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
            session.ephemeralKeyPair.public.toEcPublicKey(EcCurve.P256),
            connectionSetup.getConnectionOptions(),
            qrEngagementListener,
            Dispatchers.IO.asExecutor()
        )
            .setConnectionMethods(connectionSetup.getConnectionMethods())
            .build()

        onQrEngagementReady(qrEngagement.deviceEngagementUriEncoded)
        Napier.d(tag = TAG, message = "configure: Device Engagement Ready")
    }

    fun close() {
        try {
            qrEngagement.close()
        } catch (exception: RuntimeException) {
            Napier.d(tag = TAG, message = "Error closing QR engagement $exception")
        }
    }
}
