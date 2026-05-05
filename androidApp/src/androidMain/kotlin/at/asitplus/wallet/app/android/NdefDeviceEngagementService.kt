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
import at.asitplus.wallet.app.common.createErrorReportingScope
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import at.asitplus.wallet.app.common.presentation.PresentmentTimeout
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT
import ui.viewmodels.authentication.PresentationStateModel
import kotlin.time.Clock
import kotlin.time.Duration

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

class NdefDeviceEngagementService : HostApduService() {
    companion object {
        val TAG = "NdefDeviceEngagementService"

        // Holds the model for the currently active engagement so SharingActivity can retrieve it.
        // Written only from the service instance that owns the current engagement.
        var currentPresentationStateModel: PresentationStateModel? = null
            private set
    }

    // All engagement state lives on the service instance, not in a companion, so each service
    // instance gets a clean slate, and there is no shared mutable state across NFC taps.
    private var serviceErrorService: ErrorService? = null
    private val serviceScope = createErrorReportingScope("NdefDeviceEngagementService") {
        serviceErrorService
    }

    private var engagement: MdocNfcEngagementHelper? = null
    private var disableEngagementJob: Job? = null
    private var listenForCancellationFromUiJob: Job? = null
    private lateinit var walletConfig: WalletConfig

    private fun vibrate(pattern: Int) = kotlin.runCatching {
        val vibrator = ContextCompat.getSystemService(applicationContext, Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createPredefined(pattern))
    }.onFailure { e -> Napier.w("Vibrating failed", e, tag = TAG) }

    private fun vibrateError() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    private fun vibrateSuccess() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)

    override fun onDestroy() {
        super.onDestroy()
        commandApduListenJob?.cancel()
        serviceScope.cancel()
        currentPresentationStateModel = null
        serviceErrorService = null
    }

    private var commandApduListenJob: Job? = null
    private val commandApduChannel = Channel<CommandApdu>(Channel.UNLIMITED)

    override fun onCreate() {
        super.onCreate()
        initializeApplication(applicationContext)
        walletConfig = WalletConfig(
            dataStoreService = RealDataStoreService(
                dataStore = getDataStore(applicationContext),
                platformAdapter = DummyPlatformAdapter()
            ),
            errorService = ErrorService(serviceScope)
        )
        serviceErrorService = walletConfig.errorService

        commandApduListenJob = serviceScope.launch {
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
        Napier.i("startNdefEngagement", tag = TAG)

        disableEngagementJob?.cancel()
        disableEngagementJob = null
        listenForCancellationFromUiJob?.cancel()
        listenForCancellationFromUiJob = null

        val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val timeStarted = Clock.System.now()

        // Create a fresh PresentationStateModel with a dedicated child scope so that
        // reset() cancels only presentment coroutines without touching serviceScope
        // (which also owns commandApduListenJob).
        val presentmentScope = CoroutineScope(
            serviceScope.coroutineContext + SupervisorJob(serviceScope.coroutineContext[Job])
                    + CoroutineName("NdefDeviceEngagementService:presentment")
        )
        val model = PresentationStateModel(presentmentScope)
        currentPresentationStateModel = model
        model.init()

        // The UI consuming [PresentationModel] may have a cancel button which will trigger
        // COMPLETED state when pressed.
        listenForCancellationFromUiJob = presentmentScope.launch {
            model.state.collect { state ->
                if (state == PresentationStateModel.State.COMPLETED) {
                    engagement = null
                    disableEngagementJob?.cancel()
                    disableEngagementJob = null
                }
            }
        }

        val intent = Intent(applicationContext, SharingActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        intent.action = PRESENTATION_REQUESTED_INTENT
        applicationContext.startActivity(intent)

        fun negotiatedHandoverPicker(connectionMethods: List<MdocConnectionMethod>): MdocConnectionMethod {
            Napier.i("Negotiated Handover available methods: $connectionMethods", tag = TAG)
            for (prefix in walletConfig.presentmentNegotiatedHandoverPreferredOrder) {
                for (connectionMethod in connectionMethods) {
                    if (connectionMethod.toString().startsWith(prefix)) {
                        Napier.i("Using method $connectionMethod", tag = TAG)
                        return connectionMethod
                    }
                }
            }
            Napier.i("Fallback, using method ${connectionMethods.first()}", tag = TAG)
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
                Napier.d("Waiting for start", tag = TAG)
                vibrateSuccess()
                model.start(connectionMethods.any { it is MdocConnectionMethodBle })

                val duration = Clock.System.now() - timeStarted
                listenOnMethods(
                    model = model,
                    connectionMethods = connectionMethods,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = handover,
                    eDeviceKey = ephemeralDeviceKey,
                    engagementDuration = duration
                )
            },
            onError = { error ->
                Napier.w("Engagement failed", error, tag = TAG)
                vibrateError()
                model.setCompleted(error)
                engagement = null
            },
            staticHandoverMethods = staticHandoverConnectionMethods,
            negotiatedHandoverPicker = negotiatedHandoverPicker
        )
    }

    private fun listenOnMethods(
        model: PresentationStateModel,
        connectionMethods: List<MdocConnectionMethod>,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        eDeviceKey: EcPrivateKey,
        engagementDuration: Duration,
    ) {
        model.presentmentScope.launch {
            Napier.d("Waiting for state", tag = TAG)
            model.state.first {
                it != PresentationStateModel.State.IDLE
                        && it != PresentationStateModel.State.NO_PERMISSION
                        && it != PresentationStateModel.State.CHECK_PERMISSIONS
            }
            Napier.d("${model.state.value} reached, wait for connection using main transport", tag = TAG)
            val advertisedTransports = connectionMethods.advertise(
                role = MdocRole.MDOC,
                transportFactory = MdocTransportFactory.Default,
                options = MdocTransportOptions(
                    bleUseL2CAP = walletConfig.bleUseL2CAPEnabled.first(),
                    bleUseL2CAPInEngagement = walletConfig.bleUseL2CAPInEngagementEnabled.first()
                )
            )

            val transport = advertisedTransports.waitForConnection(eDeviceKey.publicKey)
            model.setMechanism(
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
        Napier.d("processCommandApdu, started = $started", tag = TAG)

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
            Napier.e("processCommandApdu", e, tag = TAG)
            e.printStackTrace()
        }
        return null
    }

    // Called by OS when an APDU arrives
    override fun processCommandApdu(encodedCommandApdu: ByteArray, extras: Bundle?): ByteArray? {
        commandApduChannel.trySend(CommandApdu.decode(encodedCommandApdu))
        return null
    }

    override fun onDeactivated(reason: Int) {
        Napier.i("onDeactivated: reason=$reason", tag = TAG)
        started = false
        if (engagement == null) {
            Napier.d("NdefDeviceEngagementService: Engagement is not running")
            return
        }

        val model = currentPresentationStateModel ?: return

        // If the reader hasn't connected by the time NFC interaction ends, make sure we only
        // wait for a limited amount of time.
        if (model.state.value == PresentationStateModel.State.CONNECTING) {
            disableEngagementJob = serviceScope.launch(CoroutineName("NdefDeviceEngagementService:onDeactivated")) {
                try {
                    model.waitForConnectionUsingMainTransport(walletConfig.connectionTimeout.first())
                    Napier.d("NdefDeviceEngagementService: Main transport connected")
                } catch (_: TimeoutCancellationException) {
                    val message =
                        "NdefDeviceEngagementService: Reader didn't connect in ${walletConfig.connectionTimeout.first()}, closing"
                    Napier.w(message)
                    model.setCompleted(PresentmentTimeout(message))
                }
                engagement = null
                disableEngagementJob = null
            }
        }
    }
}