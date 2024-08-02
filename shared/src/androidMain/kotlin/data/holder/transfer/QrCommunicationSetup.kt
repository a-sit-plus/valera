package com.android.mdl.app.transfer

import android.content.Context
import android.util.Log
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.legacy.PresentationSession
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import data.holder.transfer.ConnectionSetup
import data.holder.transfer.CredentialStore
import data.holder.transfer.SessionSetup
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

        override fun onDeviceEngagementReady() {
            Log.d(TAG, "QR Engagement: Device Engagement Ready")
            onQrEngagementReady(deviceEngagementUriEncoded)
        }

        override fun onDeviceConnecting() {
            Log.d(TAG, "QR Engagement: Device Connecting")
            onConnecting()
        }

        override fun onDeviceConnected(transport: DataTransport) {
            if (deviceRetrievalHelper != null) {
                Log.d(TAG, "OnDeviceConnected for QR engagement -> ignoring due to active presentation")
                return
            }
            Log.d(TAG, "OnDeviceConnected via QR: qrEngagement=$qrEngagement")
            val builder = DeviceRetrievalHelper.Builder(
                context,
                deviceRetrievalHelperListener,
                Dispatchers.IO.asExecutor(),//context.mainExecutor(),
                session.ephemeralKeyPair
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
            Log.d(TAG, "QR onError: ${error.message}")
            onCommunicationError(error)
        }
    }

    private val deviceRetrievalHelperListener = object : DeviceRetrievalHelper.Listener {
        override fun onEReaderKeyReceived(eReaderKey: PublicKey) {
            Log.d(TAG, "DeviceRetrievalHelper Listener (QR): OnEReaderKeyReceived")
            session.setSessionTranscript(deviceRetrievalHelper!!.sessionTranscript)
            session.setReaderEphemeralPublicKey(eReaderKey)
        }

        override fun onDeviceRequest(deviceRequestBytes: ByteArray) {
            Log.d(TAG, "DeviceRetrievalHelper Listener (QR): OnDeviceRequest")
            onNewDeviceRequest(deviceRequestBytes)
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            Log.d(TAG, "DeviceRetrievalHelper Listener (QR): onDeviceDisconnected")
            onDisconnected(transportSpecificTermination)
        }

        override fun onError(error: Throwable) {
            Log.d(TAG, "DeviceRetrievalHelper Listener (QR): onError -> ${error.message}")
            onCommunicationError(error)
        }
    }

    private lateinit var qrEngagement: QrEngagementHelper
    private var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    val deviceEngagementUriEncoded: String
        get() = qrEngagement.deviceEngagementUriEncoded

    fun configure() {
        qrEngagement =
            QrEngagementHelper.Builder(
                context,
                session.ephemeralKeyPair.public,
                connectionSetup.getConnectionOptions(),
                qrEngagementListener,
                Dispatchers.IO.asExecutor()//context.mainExecutor(),
            )
                .setConnectionMethods(connectionSetup.getConnectionMethods())
                .build()
    }

    fun close() {
        try {
            qrEngagement.close()
        } catch (exception: RuntimeException) {
            Log.d(TAG, "Error closing QR engagement", exception)
        }
    }
}