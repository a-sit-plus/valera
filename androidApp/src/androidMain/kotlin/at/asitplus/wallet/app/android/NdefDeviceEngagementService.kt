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
import kotlinx.coroutines.flow.MutableStateFlow
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

        // True once engagement handover is complete; the activity observes this to switch the
        // preferred HCE service from the engagement service to the data retrieval service.
        val nfcDataTransferActive = MutableStateFlow(false)

        @Volatile
        private var activeSessionId: String? = null
        @Volatile
        private var activeEngagement: MdocNfcEngagementHelper? = null
        @Volatile
        private var activeDisableEngagementJob: Job? = null
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

        private fun clearActiveEngagement(reason: String) {
            activeEngagement = null
            activeStarted = false
            activeBleHandoverPending = false
            activePresentationUiLaunched = false
            activeDisableEngagementJob?.cancel()
            activeDisableEngagementJob = null
            nfcDataTransferActive.value = false
            Napier.d("NdefDeviceEngagementService cleared active engagement reason=$reason", tag = TAG)
        }
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
        activeBleHandoverPending = false
        activePresentationUiLaunched = false
        nfcDataTransferActive.value = false

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
                Napier.d("Waiting for start", tag = TAG)
                vibrateSuccess()
                nfcDataTransferActive.value = true
                activeBleHandoverPending = connectionMethods.any { it is MdocConnectionMethodBle }
                model.start(activeBleHandoverPending)
                if (activeBleHandoverPending) {
                    if (hasBlePermissions()) {
                        Napier.d("BLE permissions already granted, continuing without foreground UI", tag = TAG)
                        model.setPermissionState(true)
                    } else {
                        Napier.d("BLE permissions missing, launching UI to request them", tag = TAG)
                        launchPresentationUiIfNeeded("missing-ble-permission")
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
            val advertisedTransports = connectionMethods.advertise(
                role = MdocRole.MDOC,
                transportFactory = MdocTransportFactory.Default,
                options = MdocTransportOptions(
                    bleUseL2CAP = walletConfig.bleUseL2CAPEnabled.first(),
                    bleUseL2CAPInEngagement = walletConfig.bleUseL2CAPInEngagementEnabled.first()
                )
            )

            val transport = advertisedTransports.waitForConnection(eDeviceKey.publicKey)
            activeBleHandoverPending = false
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
            activeDisableEngagementJob?.cancel()
            activeDisableEngagementJob = null
            activeEngagement = null
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
            Napier.d("NdefDeviceEngagementService: Engagement is not running")
            return
        }

        val model = currentPresentationStateModel ?: return

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
                activeDisableEngagementJob = activePresentmentScope.launch(CoroutineName("NdefDeviceEngagementService:onDeactivated")) {
                    try {
                        model.waitForConnectionUsingMainTransport(walletConfig.connectionTimeout.first())
                        Napier.d("NdefDeviceEngagementService: Main transport connected")
                    } catch (_: TimeoutCancellationException) {
                        val message =
                            "NdefDeviceEngagementService: Reader didn't connect in ${walletConfig.connectionTimeout.first()}, closing"
                        Napier.w(message)
                        model.setCompleted(PresentmentTimeout(message))
                    }
                    clearActiveEngagement("deactivated-timeout")
                }
            }
            else -> {}
        }
    }
}
