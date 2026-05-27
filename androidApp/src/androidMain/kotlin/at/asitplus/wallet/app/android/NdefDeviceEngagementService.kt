package at.asitplus.wallet.app.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.createErrorReportingScope
import at.asitplus.wallet.app.common.presentation.LocalPresentmentBusyException
import at.asitplus.wallet.app.common.presentation.LocalPresentmentEngagementMethod
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSource
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import at.asitplus.wallet.app.common.presentation.PresentmentTimeout
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import org.multipaz.mdoc.transport.NfcTransportMdoc
import org.multipaz.mdoc.transport.advertise
import org.multipaz.mdoc.transport.waitForConnection
import org.multipaz.nfc.CommandApdu
import org.multipaz.nfc.ResponseApdu
import org.multipaz.util.UUID
import org.koin.core.context.GlobalContext
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT
import ui.viewmodels.authentication.PresentationStateModel
import kotlin.time.Clock
import kotlin.time.Duration

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

class NdefDeviceEngagementService : HostApduService() {
    companion object {
        val TAG = "NdefDeviceEngagementService"

        // Written from the NFC APDU callback thread; read from the main thread in
        // TransientFlowActivity.populateLink(). @Volatile ensures the write is visible
        // across cores without an explicit lock.
        @Volatile
        var currentPresentationStateModel: PresentationStateModel? = null
            private set

        @Volatile
        private var activeSessionId: String? = null
        @Volatile
        private var activeEngagement: MdocNfcEngagementHelper? = null
        @Volatile
        private var activeDisableEngagementJob: Job? = null
        @Volatile
        private var activeNfcTransportCleanupJob: Job? = null
        @Volatile
        private var activeBleHandoverPending = false
        @Volatile
        private var activePresentationUiLaunched = false
        @Volatile
        private var activeStarted = false
        private val activePresentmentScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineName("NdefDeviceEngagementService:presentment")
        )

        private fun localPresentmentSessionCoordinator(): LocalPresentmentSessionCoordinator =
            GlobalContext.get().get()

        private fun setCurrentPresentationStateModel(model: PresentationStateModel?) {
            currentPresentationStateModel = model
            Napier.d(
                "NdefDeviceEngagementService currentPresentationStateModel=" +
                        "${model != null} hash=${model?.hashCode()}",
                tag = TAG
            )
        }

        private fun clearCurrentPresentationStateModel(
            model: PresentationStateModel? = null,
            reason: String
        ) {
            if (model != null && currentPresentationStateModel !== model) {
                Napier.d(
                    "NdefDeviceEngagementService not clearing stale model for reason=$reason " +
                            "currentHash=${currentPresentationStateModel?.hashCode()} requestedHash=${model.hashCode()}",
                    tag = TAG
                )
                return
            }
            currentPresentationStateModel = null
            activeSessionId = null
            Napier.d("NdefDeviceEngagementService cleared currentPresentationStateModel reason=$reason", tag = TAG)
        }

        private fun startConnectingTimeout(model: PresentationStateModel, walletConfig: WalletConfig) {
            if (activeDisableEngagementJob != null) return
            Napier.d("NdefDeviceEngagementService: starting connecting timeout", tag = TAG)
            activeDisableEngagementJob = activePresentmentScope.launch(CoroutineName("NdefDeviceEngagementService:connectingTimeout")) {
                try {
                    model.waitForConnectionUsingMainTransport(walletConfig.connectionTimeout.first())
                    Napier.d("NdefDeviceEngagementService: Main transport connected")
                    // Success: session-cleanup and listenOnMethods handle teardown.
                } catch (_: TimeoutCancellationException) {
                    val message =
                        "NdefDeviceEngagementService: Reader didn't connect in ${walletConfig.connectionTimeout.first()}, closing"
                    Napier.w(message, tag = TAG)
                    model.setCompleted(PresentmentTimeout(message))
                    clearActiveEngagement("connecting-timeout")
                }
            }
        }

        private fun clearActiveEngagement(reason: String) {
            val wasNfcDataTransferActive = NfcTransferState.nfcDataTransferActive.value
            activeEngagement = null
            activeStarted = false
            activeBleHandoverPending = false
            activePresentationUiLaunched = false
            activeDisableEngagementJob?.cancel()
            activeDisableEngagementJob = null
            activeNfcTransportCleanupJob?.cancel()
            activeNfcTransportCleanupJob = null
            if (wasNfcDataTransferActive) {
                activeNfcTransportCleanupJob = activePresentmentScope.launch(
                    CoroutineName("NdefDeviceEngagementService:nfcTransportCleanup")
                ) {
                    Napier.i(
                        "Delaying NFC data-transfer cleanup after $reason so reader can finish APDU exchange",
                        tag = TAG
                    )
                    delay(NFC_TRANSPORT_CLEANUP_DELAY_MS)
                    runCatching { NfcTransportMdoc.onDeactivated() }
                        .onFailure { Napier.w("NFC transport cleanup failed", it, tag = TAG) }
                    NfcTransferState.nfcDataTransferActive.value = false
                    activeNfcTransportCleanupJob = null
                    Napier.i("NFC data-transfer cleanup finished after $reason", tag = TAG)
                }
            } else {
                runCatching { NfcTransportMdoc.onDeactivated() }
                    .onFailure { Napier.w("NFC transport cleanup failed", it, tag = TAG) }
                NfcTransferState.nfcDataTransferActive.value = false
            }
            Napier.d("NdefDeviceEngagementService cleared active engagement reason=$reason", tag = TAG)
        }

        private const val NFC_TRANSPORT_CLEANUP_DELAY_MS = 2_000L
    }

    private var serviceErrorService: ErrorService? = null
    private val serviceScope = createErrorReportingScope("NdefDeviceEngagementService") {
        serviceErrorService
    }

    private lateinit var walletConfig: WalletConfig

    private fun vibrate(pattern: Int) = kotlin.runCatching {
        val vibrator = ContextCompat.getSystemService(applicationContext, Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createPredefined(pattern))
    }.onFailure { e -> Napier.w("Vibrating failed", e, tag = TAG) }

    private fun vibrateError() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    private fun vibrateSuccess() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)

    private fun launchPresentationUiIfNeeded(reason: String) {
        if (activePresentationUiLaunched) {
            Napier.d("Presentation UI already launched reason=$reason", tag = TAG)
            return
        }
        activePresentationUiLaunched = true
        Napier.d("Launching presentation UI reason=$reason", tag = TAG)
        val intent = Intent(applicationContext, TransientFlowActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        intent.action = PRESENTATION_REQUESTED_INTENT
        applicationContext.startActivity(intent)
    }

    private fun hasBlePermissions(): Boolean = listOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
    ).all { permission ->
        ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        Napier.d(
            "NdefDeviceEngagementService.onDestroy currentPresentationStateModel=" +
                    "${currentPresentationStateModel != null} hash=${currentPresentationStateModel?.hashCode()}",
            tag = TAG
        )
        commandApduListenJob?.cancel()
        serviceScope.cancel()
        serviceErrorService = null

        // On some devices onDestroy fires without a prior onDeactivated call. Ensure the
        // CONNECTING-state timeout is started so the session does not hang forever.
        val model = currentPresentationStateModel ?: return
        when (model.state.value) {
            PresentationStateModel.State.INITIALISING -> {
                val message = "NdefDeviceEngagementService: Service destroyed during INITIALISING, aborting"
                Napier.w(message, tag = TAG)
                model.setCompleted(PresentmentTimeout(message))
                clearActiveEngagement("onDestroy-initialising")
            }
            PresentationStateModel.State.CONNECTING -> {
                Napier.d(
                    "NdefDeviceEngagementService: onDestroy in CONNECTING without prior onDeactivated, starting timeout",
                    tag = TAG
                )
                startConnectingTimeout(model, walletConfig)
            }
            else -> {}
        }
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

        activeDisableEngagementJob?.cancel()
        activeDisableEngagementJob = null
        activeNfcTransportCleanupJob?.cancel()
        activeNfcTransportCleanupJob = null
        activeBleHandoverPending = false
        activePresentationUiLaunched = false
        runCatching { NfcTransportMdoc.onDeactivated() }
            .onFailure { Napier.w("Pre-engagement NFC transport cleanup failed", it, tag = TAG) }
        Napier.i("Resetting NFC data-transfer active flag before starting NDEF engagement", tag = TAG)
        NfcTransferState.nfcDataTransferActive.value = false

        val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val timeStarted = Clock.System.now()
        val session = localPresentmentSessionCoordinator().startSession(
            source = LocalPresentmentSource.ANDROID_EXTERNAL_NFC,
            engagementMethod = LocalPresentmentEngagementMethod.NFC,
        )
        activeSessionId = session.sessionId
        val model = session.presentationStateModel
        setCurrentPresentationStateModel(model)
        model.init()
        localPresentmentSessionCoordinator().registerCleanup(session.sessionId) {
            clearActiveEngagement("session-cleanup")
            clearCurrentPresentationStateModel(reason = "session-cleanup")
        }
        model.presentmentScope.launch {
            model.state.first { it == PresentationStateModel.State.COMPLETED }
            activeSessionId?.let { sessionId ->
                localPresentmentSessionCoordinator().finishSession(sessionId, "external-presentation-completed")
            }
        }

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

        activeEngagement = MdocNfcEngagementHelper(
            eDeviceKey = ephemeralDeviceKey.publicKey,
            onHandoverComplete = { connectionMethods, encodedDeviceEngagement, handover ->
                Napier.i(
                    "NFC engagement handover complete; methods=$connectionMethods, " +
                            "handover=${handover::class.simpleName}, deviceEngagementBytes=${encodedDeviceEngagement.size}",
                    tag = TAG
                )
                vibrateSuccess()
                Napier.i("Switching preferred HCE service to NFC data retrieval after handover", tag = TAG)
                NfcTransferState.nfcDataTransferActive.value = true
                activeBleHandoverPending = connectionMethods.any { it is MdocConnectionMethodBle }
                Napier.i("Starting presentation model after NFC handover; blePending=$activeBleHandoverPending", tag = TAG)
                model.start(activeBleHandoverPending)
                if (activeBleHandoverPending) {
                    if (hasBlePermissions()) {
                        Napier.d("BLE permissions already granted, continuing without foreground UI", tag = TAG)
                        model.setPermissionState(true)
                    } else {
                        Napier.d("BLE permissions missing, launching UI to request them", tag = TAG)
                        //launchPresentationUiIfNeeded("missing-ble-permission")
                    }
                }

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
                clearActiveEngagement("engagement-error")
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
            // Launch the UI now so the user sees the connecting state and any subsequent
            // errors (e.g. timeout) rather than a silent hang.
            //launchPresentationUiIfNeeded("connecting")
            Napier.i("Advertising main transports after NFC handover; methods=$connectionMethods", tag = TAG)
            val advertisedTransports = connectionMethods.advertise(
                role = MdocRole.MDOC,
                transportFactory = MdocTransportFactory.Default,
                options = MdocTransportOptions(
                    bleUseL2CAP = walletConfig.bleUseL2CAPEnabled.first(),
                    bleUseL2CAPInEngagement = walletConfig.bleUseL2CAPInEngagementEnabled.first()
                )
            )

            Napier.i("Waiting for main transport connection after NFC handover", tag = TAG)
            val transport = advertisedTransports.waitForConnection(eDeviceKey.publicKey)
            Napier.i(
                "Main transport connected after NFC handover: ${transport::class.simpleName}, state=${transport.state.value}",
                tag = TAG
            )
            activeBleHandoverPending = false
            // Cancel the connecting timeout BEFORE setMechanism() changes the state.
            // The timeout job watches for state to leave CONNECTING; if we cancel after
            // setMechanism(), there is a race where the job calls clearActiveEngagement()
            // (resetting nfcDataTransferActive) before cancel() takes effect.
            activeDisableEngagementJob?.cancel()
            activeDisableEngagementJob = null
            activeEngagement = null
            Napier.i("Setting presentation mechanism for NFC-engaged local presentment", tag = TAG)
            model.setMechanism(
                MdocPresentmentMechanism(
                    transport = transport,
                    ephemeralDeviceKey = eDeviceKey,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = handover,
                    engagementDuration = engagementDuration,
                    // The NFC-triggered local presentation flow is a single exchange. Keeping the
                    // BLE transport open for follow-up requests leaves the UI stuck waiting after
                    // the verifier has already received the response.
                    allowMultipleRequests = false
                )
            )
            launchPresentationUiIfNeeded("transport-connected")
        }
    }

    private suspend fun processCommandApdu(commandApdu: CommandApdu): ResponseApdu? {
        Napier.d("processCommandApdu, started = $activeStarted", tag = TAG)

        if (!activeStarted) {
            try {
                activeStarted = true
                startEngagement()
            } catch (error: LocalPresentmentBusyException) {
                activeStarted = false
                Napier.w("Rejecting new NFC engagement while another presentment is active", error, tag = TAG)
                return null
            }
        }

        try {
            activeEngagement?.let {
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
        activeStarted = false
        if (activeEngagement == null) {
            Napier.d(
                "NdefDeviceEngagementService: Engagement is not running; " +
                        "nfcDataTransferActive=${NfcTransferState.nfcDataTransferActive.value}",
                tag = TAG
            )
            return
        }

        val model = currentPresentationStateModel ?: return
        Napier.i(
            "NDEF engagement service deactivated; modelState=${model.state.value}, " +
                    "blePending=$activeBleHandoverPending, " +
                    "nfcDataTransferActive=${NfcTransferState.nfcDataTransferActive.value}",
            tag = TAG
        )

        when (model.state.value) {
            PresentationStateModel.State.INITIALISING -> {
                // NFC link lost before handover completed — the session coordinator would
                // block all subsequent engagements without this explicit abort.
                val message = "NdefDeviceEngagementService: NFC link lost during engagement, closing"
                Napier.w(message, tag = TAG)
                model.setCompleted(PresentmentTimeout(message))
                clearActiveEngagement("deactivated-initialising")
            }
            PresentationStateModel.State.CONNECTING -> {
                // Handover completed but main transport not yet connected — give the reader a
                // limited window to connect before giving up.
                Napier.d(
                    "NdefDeviceEngagementService: NFC link ended while waiting for main transport; blePending=$activeBleHandoverPending",
                    tag = TAG
                )
                startConnectingTimeout(model, walletConfig)
            }
            else -> {}
        }
    }
}
