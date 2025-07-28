package at.asitplus.wallet.app.android

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import at.asitplus.wallet.app.common.presentation.PresentmentTimeout
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.DataItem
import org.multipaz.context.initializeApplication
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.nfc.MdocNfcEngagementHelper
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.advertise
import org.multipaz.mdoc.transport.waitForConnection
import org.multipaz.nfc.CommandApdu
import org.multipaz.nfc.ResponseApdu
import org.multipaz.util.UUID
import ui.navigation.PRESENTATION_REQUESTED_INTENT
import ui.viewmodels.authentication.PresentationStateModel
import kotlin.time.Duration


// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

class NdefDeviceEngagementService : HostApduService() {
    companion object {
        private var engagement: MdocNfcEngagementHelper? = null
        private var disableEngagementJob: Job? = null
        private var listenForCancellationFromUiJob: Job? = null
        private lateinit var walletConfig: WalletConfig

        // TODO use error service to show error to user, but how to get it from here?
        private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            Napier.e("FAILURE IN COROUTINE", error)
        }

        private val coroutineScope =
            CoroutineScope(Dispatchers.Default + CoroutineName("NdefDeviceEngagementService") + coroutineExceptionHandler)

        val presentationStateModel: PresentationStateModel by lazy {
            PresentationStateModel(coroutineScope)
        }
    }

    private fun vibrate(pattern: Int) = kotlin.runCatching {
        val vibrator = ContextCompat.getSystemService(
            applicationContext,
            Vibrator::class.java
        )

        val effect = VibrationEffect.createPredefined(pattern)
        vibrator?.vibrate(effect)
    }.onFailure { e -> Napier.w("Vibrating failed", e) }

    private fun vibrateError() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)

    private fun vibrateSuccess() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)

    override fun onDestroy() {
        super.onDestroy()
        commandApduListenJob?.cancel()
    }

    private var commandApduListenJob: Job? = null
    private val commandApduChannel = Channel<CommandApdu>(Channel.UNLIMITED)

    override fun onCreate() {
        super.onCreate()
        initializeApplication(applicationContext)
        walletConfig = WalletConfig(
            dataStoreService = RealDataStoreService(getDataStore(applicationContext), DummyPlatformAdapter()),
            errorService = ErrorService()
        )

        commandApduListenJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val commandApdu = commandApduChannel.receive()
                val responseApdu = processCommandApdu(commandApdu)
                if (responseApdu != null) {
                    sendResponseApdu(responseApdu.encode())
                }
            }
        }
    }

    private var started = false

    private suspend fun startEngagement() {
        Napier.i("NdefDeviceEngagementService: startNdefEngagement")

        disableEngagementJob?.cancel()
        disableEngagementJob = null
        listenForCancellationFromUiJob?.cancel()
        listenForCancellationFromUiJob = null

        val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val timeStarted = Clock.System.now()

        presentationStateModel.reset()
        presentationStateModel.init()

        // The UI consuming [PresentationModel] may
        // have a cancel button which will trigger COMPLETED state when pressed.
        //
        listenForCancellationFromUiJob = presentationStateModel.presentmentScope.launch {
            presentationStateModel.state
                .collect { state ->
                    if (state == PresentationStateModel.State.COMPLETED) {
                        engagement = null
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

        suspend fun negotiatedHandoverPicker(connectionMethods: List<MdocConnectionMethod>): MdocConnectionMethod {
            Napier.i("NdefDeviceEngagementService: Negotiated Handover available methods: $connectionMethods")
            for (prefix in walletConfig.presentmentNegotiatedHandoverPreferredOrder) {
                for (connectionMethod in connectionMethods) {
                    if (connectionMethod.toString().startsWith(prefix)
                        && walletConfig.isConnectionMethodEnabled(prefix = prefix)
                    ) {
                        Napier.i("NdefDeviceEngagementService: Using method $connectionMethod")
                        return connectionMethod
                    }
                }
            }
            Napier.i("NdefDeviceEngagementService: Fallback, using method ${connectionMethods.first()}")
            return connectionMethods.first()
        }

        val negotiatedHandoverPicker: ((connectionMethods: List<MdocConnectionMethod>) -> MdocConnectionMethod)? =
            if (walletConfig.presentmentUseNegotiatedHandover.first()) {
                { connectionMethods -> runBlocking { negotiatedHandoverPicker(connectionMethods) } }
            } else {
                null
            }

        var staticHandoverConnectionMethods: List<MdocConnectionMethod>? = null
        if (!walletConfig.presentmentUseNegotiatedHandover.first()) {
            staticHandoverConnectionMethods = mutableListOf()
            val bleUuid = UUID.randomUUID()
            if (walletConfig.presentmentBleCentralClientModeEnabled.first()) {
                staticHandoverConnectionMethods.add(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = false,
                        supportsCentralClientMode = true,
                        peripheralServerModeUuid = null,
                        centralClientModeUuid = bleUuid,
                    )
                )
            }
            if (walletConfig.presentmentBlePeripheralServerModeEnabled.first()) {
                staticHandoverConnectionMethods.add(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = true,
                        supportsCentralClientMode = false,
                        peripheralServerModeUuid = bleUuid,
                        centralClientModeUuid = null,
                    )
                )
            }
            if (walletConfig.presentmentNfcDataTransferEnabled.first()) {
                staticHandoverConnectionMethods.add(
                    MdocConnectionMethodNfc(
                        commandDataFieldMaxLength = 0xffff,
                        responseDataFieldMaxLength = 0x10000
                    )
                )
            }
        }

        engagement = MdocNfcEngagementHelper(
            eDeviceKey = ephemeralDeviceKey.publicKey,
            onHandoverComplete = { connectionMethods, encodedDeviceEngagement, handover ->
                Napier.d("NdefDeviceEngagementService: Waiting for start")
                vibrateSuccess()
                presentationStateModel.start(connectionMethods.any { it is MdocConnectionMethodBle })

                val duration = Clock.System.now() - timeStarted
                listenOnMethods(
                    connectionMethods = connectionMethods,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = handover,
                    eDeviceKey = ephemeralDeviceKey,
                    engagementDuration = duration
                )
            },
            onError = { error ->
                Napier.w("NdefDeviceEngagementService: Engagement failed", error)
                vibrateError()
                presentationStateModel.setCompleted(error)
                engagement = null
            },
            staticHandoverMethods = staticHandoverConnectionMethods,
            negotiatedHandoverPicker = negotiatedHandoverPicker
        )
    }

    private fun listenOnMethods(
        connectionMethods: List<MdocConnectionMethod>,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        eDeviceKey: EcPrivateKey,
        engagementDuration: Duration,
    ) {
        presentationStateModel.presentmentScope.launch {
            Napier.d("NdefDeviceEngagementService: Waiting for state")
            presentationStateModel.state.first { it != PresentationStateModel.State.IDLE && it != PresentationStateModel.State.NO_PERMISSION && it != PresentationStateModel.State.CHECK_PERMISSIONS }
            Napier.d("${presentationStateModel.state.value} reached, wait for connection using main transport")
            // First advertise the connection methods
            val advertisedTransports = connectionMethods.advertise(
                role = MdocRole.MDOC,
                transportFactory = MdocTransportFactory.Default,
                options = MdocTransportOptions(
                    bleUseL2CAP = walletConfig.readerBleL2CapEnabled.first()
                )
            )

            // Then wait for connection
            val transport = advertisedTransports.waitForConnection(
                eSenderKey = eDeviceKey.publicKey
            )
            presentationStateModel.setMechanism(
                MdocPresentmentMechanism(
                    transport = transport,
                    ephemeralDeviceKey = eDeviceKey,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = handover,
                    engagementDuration = engagementDuration,
                    allowMultipleRequests = walletConfig.presentmentAllowMultipleRequests.first()
                )
            )
            disableEngagementJob?.cancel()
            disableEngagementJob = null
            listenForCancellationFromUiJob?.cancel()
            listenForCancellationFromUiJob = null
            engagement = null
        }
    }

    private suspend fun processCommandApdu(commandApdu: CommandApdu): ResponseApdu? {
        Napier.d("NdefDeviceEngagementService: processCommandApdu, started = $started")

        if (!started) {
            started = true
            startEngagement()
        }

        try {
            engagement?.let {
                val responseApdu = it.processApdu(commandApdu)
                return responseApdu
            }
        } catch (e: Throwable) {
            Napier.e("NdefDeviceEngagementService: processCommandApdu", e)
            e.printStackTrace()
        }
        return null
    }

    // Called by OS when an APDU arrives
    override fun processCommandApdu(encodedCommandApdu: ByteArray, extras: Bundle?): ByteArray? {
        // Bounce the APDU to processCommandApdu() above via the coroutine in I/O thread set up in onCreate()
        commandApduChannel.trySend(CommandApdu.decode(encodedCommandApdu))
        return null
    }

    override fun onDeactivated(reason: Int) {
        Napier.i("NdefDeviceEngagementService: onDeactivated: reason=$reason")
        started = false
        // If the reader hasn't connected by the time NFC interaction ends, make sure we only
        // wait for a limited amount of time.
        if (engagement == null) {
            Napier.d("NdefDeviceEngagementService: Engagement is not running")
            return
        }
        disableEngagementJob = CoroutineScope(Dispatchers.IO + CoroutineName("NdefDeviceEngagementService: onDeactivated")).launch {
            try {
                presentationStateModel.waitForConnectionUsingMainTransport(walletConfig.connectionTimeout.first())
                Napier.d("NdefDeviceEngagementService: Main transport connected")
            } catch (_: TimeoutCancellationException) {
                val message =
                    "NdefDeviceEngagementService: Reader didn't connect in ${walletConfig.connectionTimeout.first()}, closing"
                Napier.w(message)
                presentationStateModel.setCompleted(PresentmentTimeout(message))
            }
            engagement = null
            disableEngagementJob = null
        }
    }
}
