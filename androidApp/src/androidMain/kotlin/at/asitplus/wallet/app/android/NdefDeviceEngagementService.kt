package at.asitplus.wallet.app.android

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.Vibrator
import androidx.core.content.ContextCompat
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import ui.viewmodels.PresentationStateModel
import at.asitplus.wallet.app.common.presentation.PresentmentTimeout
import at.asitplus.wallet.app.common.presentation.TransferSettings
import com.android.identity.cbor.DataItem
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcCurve
import com.android.identity.crypto.EcPrivateKey
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.connectionmethod.ConnectionMethodBle
import com.android.identity.mdoc.nfc.MdocNfcEngagementHelper
import com.android.identity.mdoc.transport.MdocTransport
import com.android.identity.mdoc.transport.MdocTransportFactory
import com.android.identity.mdoc.transport.MdocTransportOptions
import com.android.identity.nfc.CommandApdu
import com.android.identity.mdoc.transport.advertiseAndWait
import com.android.identity.mdoc.connectionmethod.ConnectionMethodNfc
import com.android.identity.util.AndroidContexts
import com.android.identity.util.UUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.io.bytestring.ByteString
import ui.navigation.PRESENTATION_REQUESTED_INTENT
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NdefDeviceEngagementService: HostApduService() {
    companion object {
        private var engagement: MdocNfcEngagementHelper? = null
        private var disableEngagementJob: Job? = null
        private var listenForCancellationFromUiJob: Job? = null
        val presentationStateModel: PresentationStateModel by lazy { PresentationStateModel() }
    }


    private lateinit var settings: TransferSettings


    private fun vibrate(pattern: List<Int>) {
        val vibrator = ContextCompat.getSystemService(
            AndroidContexts.applicationContext,
            Vibrator::class.java
        )
        vibrator?.vibrate(pattern.map { it.toLong() }.toLongArray(), -1)
    }

    private fun vibrateError() {
        vibrate(listOf(0, 500))
    }

    private fun vibrateSuccess() {
        vibrate(listOf(0, 100, 50, 100))
    }

    override fun onCreate() {
        super.onCreate()
        settings = TransferSettings()
        AndroidContexts.setApplicationContext(applicationContext)
        //TODO set device engagement preferences

    }

    private var started = false

    private fun startEngagement() {
        Napier.i("NdefDeviceEngagementService: startNdefEngagement")

        disableEngagementJob?.cancel()
        disableEngagementJob = null
        listenForCancellationFromUiJob?.cancel()
        listenForCancellationFromUiJob = null

        val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val timeStarted = Clock.System.now()

        presentationStateModel.reset()
        presentationStateModel.setConnecting()

        // The UI consuming [PresentationModel] - for example the [Presentment] composable in this library - may
        // have a cancel button which will trigger COMPLETED state when pressed. Need to listen for that.
        //
        listenForCancellationFromUiJob = presentationStateModel.presentmentScope.launch {
            presentationStateModel.state
                .collect { state ->
                    if (state == PresentationStateModel.State.COMPLETED) {
                        disableEngagementJob?.cancel()
                        disableEngagementJob = null
                    }
                }
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        intent.action = PRESENTATION_REQUESTED_INTENT
        applicationContext.startActivity(intent)

        fun negotiatedHandoverPicker(connectionMethods: List<ConnectionMethod>): ConnectionMethod {
            Napier.i( "NdefDeviceEngagementService: Negotiated Handover available methods: $connectionMethods")
            for (prefix in settings.presentmentNegotiatedHandoverPreferredOrder) {
                for (connectionMethod in connectionMethods) {
                    if (connectionMethod.toString().startsWith(prefix)) {
                        Napier.i( "NdefDeviceEngagementService: Using method $connectionMethod")
                        return connectionMethod
                    }
                }
            }
            Napier.i("NdefDeviceEngagementService: Using method ${connectionMethods.first()}")
            return connectionMethods.first()
        }

        val negotiatedHandoverPicker: ((connectionMethods: List<ConnectionMethod>) -> ConnectionMethod)? =
            if (settings.presentmentUseNegotiatedHandover) {
                { connectionMethods -> negotiatedHandoverPicker(connectionMethods) }
            } else {
                null
            }

        var staticHandoverConnectionMethods: List<ConnectionMethod>? = null
        if (!settings.presentmentUseNegotiatedHandover) {
            staticHandoverConnectionMethods = mutableListOf<ConnectionMethod>()
            val bleUuid = UUID.randomUUID()
            if (settings.presentmentBleCentralClientModeEnabled) {
                staticHandoverConnectionMethods.add(
                    ConnectionMethodBle(
                        supportsPeripheralServerMode = false,
                        supportsCentralClientMode = true,
                        peripheralServerModeUuid = null,
                        centralClientModeUuid = bleUuid,
                    )
                )
            }
            if (settings.presentmentBlePeripheralServerModeEnabled) {
                staticHandoverConnectionMethods.add(
                    ConnectionMethodBle(
                        supportsPeripheralServerMode = true,
                        supportsCentralClientMode = false,
                        peripheralServerModeUuid = bleUuid,
                        centralClientModeUuid = null,
                    )
                )
            }
            if (settings.presentmentNfcDataTransferEnabled) {
                staticHandoverConnectionMethods.add(
                    ConnectionMethodNfc(
                        commandDataFieldMaxLength = 0xffff,
                        responseDataFieldMaxLength = 0x10000
                    )
                )
            }
        }

        engagement = MdocNfcEngagementHelper(
            eDeviceKey = ephemeralDeviceKey.publicKey,
            onHandoverComplete = { connectionMethods, encodedDeviceEngagement, handover ->
                vibrateSuccess()
                val duration = Clock.System.now() - timeStarted
                listenOnMethods(
                    connectionMethods = connectionMethods,
                    settings = settings,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = handover,
                    eDeviceKey = ephemeralDeviceKey,
                    engagementDuration = duration
                )
            },
            onError = { error ->
                Napier.w("NdefDeviceEngagementService: Engagement failed", error)
                error.printStackTrace()
                vibrateError()
                engagement = null
            },
            staticHandoverMethods = staticHandoverConnectionMethods,
            negotiatedHandoverPicker = negotiatedHandoverPicker
        )
    }

    private fun listenOnMethods(
        connectionMethods: List<ConnectionMethod>,
        settings: TransferSettings,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        eDeviceKey: EcPrivateKey,
        engagementDuration: Duration,
    ) {
        presentationStateModel.presentmentScope.launch {
            val transport = connectionMethods.advertiseAndWait(
                role = MdocTransport.Role.MDOC,
                transportFactory = MdocTransportFactory.Default,
                options = MdocTransportOptions(
                    bleUseL2CAP = settings.readerBleL2CapEnabled
                ),
                eSenderKey = eDeviceKey.publicKey,
                onConnectionMethodsReady = {}
            )
            presentationStateModel.setMechanism(
                MdocPresentmentMechanism(
                    transport = transport,
                    ephemeralDeviceKey = eDeviceKey,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = handover,
                    engagementDuration = engagementDuration,
                    allowMultipleRequests = settings.presentmentAllowMultipleRequests
                )
            )
            disableEngagementJob?.cancel()
            disableEngagementJob = null
            listenForCancellationFromUiJob?.cancel()
            listenForCancellationFromUiJob = null
        }
    }

    override fun processCommandApdu(encodedCommandApdu: ByteArray, extras: Bundle?): ByteArray? {
        Napier.i( "NdefDeviceEngagementService: processCommandApdu")

        if (!started) {
            started = true
            startEngagement()
        }

        try {
            engagement?.let {
                val commandApdu = CommandApdu.decode(encodedCommandApdu)
                val responseApdu = runBlocking { it.processApdu(commandApdu) }
                return responseApdu.encode()
            }
        } catch (e: Throwable) {
            Napier.e("NdefDeviceEngagementService: processCommandApdu", e)
            e.printStackTrace()
        }
        return null
    }

    override fun onDeactivated(reason: Int) {
        Napier.i( "NdefDeviceEngagementService: onDeactivated: reason=$reason")
        started = false
        // If the reader hasn't connected by the time NFC interaction ends, make sure we only
        // wait for a limited amount of time.
        if (presentationStateModel.state.value == PresentationStateModel.State.CONNECTING) {
            val timeout = 15.seconds
            Napier.i( "NdefDeviceEngagementService: Reader hasn't connected at NFC deactivation time, scheduling $timeout timeout for closing")
            disableEngagementJob = CoroutineScope(Dispatchers.IO).launch {
                delay(timeout)
                if (presentationStateModel.state.value == PresentationStateModel.State.CONNECTING) {
                    presentationStateModel.setCompleted(PresentmentTimeout("NdefDeviceEngagementService: Reader didn't connect inside $timeout, closing"))
                }
                engagement = null
                disableEngagementJob = null
            }
        }
    }
}