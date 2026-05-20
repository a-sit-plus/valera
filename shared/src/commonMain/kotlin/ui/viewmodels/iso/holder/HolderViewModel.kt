package ui.viewmodels.iso.holder

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants
import at.asitplus.wallet.app.common.iso.transfer.state.HolderState
import at.asitplus.wallet.app.common.presentation.LocalPresentmentEngagementMethod
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSource
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Simple
import org.multipaz.compose.permissions.PermissionState
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.engagement.buildDeviceEngagement
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.advertise
import org.multipaz.mdoc.transport.waitForConnection
import org.multipaz.util.UUID
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.iso.common.TransferOptionsViewModel

class HolderViewModel(
    walletMain: WalletMain,
    settingsRepository: SettingsRepository,
    private val localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator,
) : TransferOptionsViewModel(walletMain, settingsRepository) {
    val TAG = "HolderViewModel"
    private val _qrCode = MutableStateFlow<ByteString?>(null)
    val qrCode: StateFlow<ByteString?> = _qrCode.asStateFlow()
    private var activePresentmentSessionId: String? = null

    fun resetQrCode() {
        Napier.d("Reset QR code ...", tag = TAG)
        _qrCode.value = null
    }

    val onResume: () -> Unit = {
        resetPresentmentModel()
        hasBeenCalledHack = false
        resetQrCode()
        setState(HolderState.Settings)
    }

    val onConsentSettings: () -> Unit = { setState(HolderState.CheckSettings) }
    var hasBeenCalledHack: Boolean = false

    val presentationStateModel: PresentationStateModel?
        get() = activePresentmentSessionId
            ?.takeIf(localPresentmentSessionCoordinator::isSessionActive)
            ?.let { sessionId ->
                localPresentmentSessionCoordinator.activeSession()
                    ?.takeIf { it.sessionId == sessionId }
                    ?.presentationStateModel
            }

    private val _holderState = MutableStateFlow<HolderState>(HolderState.Settings)
    val holderState: StateFlow<HolderState> = _holderState

    fun setState(newState: HolderState) {
        if (_holderState.value == newState) return
        Napier.d("Change state from ${_holderState.value} to $newState", tag = TAG)
        _holderState.value = newState
    }

    fun resetPresentmentModel() {
        Napier.d("Reset presentment model ...", tag = TAG)
        activePresentmentSessionId?.let { sessionId ->
            localPresentmentSessionCoordinator.finishSession(sessionId, "holder-reset")
        }
        activePresentmentSessionId = null
    }

    fun setupPresentmentModel(
        blePermissionState: PermissionState,
        isBluetoothRequired: Boolean
    ) {
        Napier.d("Setup presentment model ...", tag = TAG)
        activePresentmentSessionId?.let { sessionId ->
            localPresentmentSessionCoordinator.finishSession(sessionId, "holder-restart")
        }
        val session = localPresentmentSessionCoordinator.startSession(
            source = LocalPresentmentSource.IN_APP_QR,
            engagementMethod = LocalPresentmentEngagementMethod.QR_CODE,
        )
        activePresentmentSessionId = session.sessionId
        session.presentationStateModel.init()
        session.presentationStateModel.start(isBluetoothRequired)
        if (isBluetoothRequired) {
            session.presentationStateModel.setPermissionState(blePermissionState.isGranted)
        }
    }

    fun markPresentmentUiAttached() {
        activePresentmentSessionId?.let(localPresentmentSessionCoordinator::markUiAttached)
    }

    fun finishPresentmentSession(reason: String) {
        activePresentmentSessionId?.let { sessionId ->
            localPresentmentSessionCoordinator.finishSession(sessionId, reason)
        }
        activePresentmentSessionId = null
    }

    fun doHolderFlow(
        isBleSelected: Boolean,
        isNfcSelected: Boolean,
        completionHandler: CompletionHandler = {}
    ) {
        val model = presentationStateModel
            ?: throw IllegalStateException("No local presentment session active")
        model.presentmentScope.launch {
            Napier.d("Do Holder flow ...", tag = TAG)
            try {
                val connectionMethods = mutableListOf<MdocConnectionMethod>()
                val bleUuid = UUID.Companion.randomUUID()

                if (isBleSelected) {
                    if (presentmentBleCentralClientModeEnabled.first()) {
                        connectionMethods.add(
                            MdocConnectionMethodBle(
                                supportsPeripheralServerMode = false,
                                supportsCentralClientMode = true,
                                peripheralServerModeUuid = null,
                                centralClientModeUuid = bleUuid,
                            )
                        )
                    }
                    if (presentmentBlePeripheralServerModeEnabled.first()) {
                        connectionMethods.add(
                            MdocConnectionMethodBle(
                                supportsPeripheralServerMode = true,
                                supportsCentralClientMode = false,
                                peripheralServerModeUuid = bleUuid,
                                centralClientModeUuid = null,
                            )
                        )
                    }
                }

                if (isNfcSelected) {
                    if (presentmentNfcDataTransferEnabled.first()) {
                        connectionMethods.add(
                            MdocConnectionMethodNfc(
                                commandDataFieldMaxLength = 0xffff,
                                responseDataFieldMaxLength = 0x10000
                            )
                        )
                    }
                }
                Napier.d("connectionMethods = $connectionMethods", tag = TAG)

                val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
                lateinit var encodedDeviceEngagement: ByteString

                // First advertise the connection methods
                val advertisedTransports = connectionMethods.advertise(
                    role = MdocRole.MDOC,
                    transportFactory = MdocTransportFactory.Default,
                    options = MdocTransportOptions(
                        bleUseL2CAP = bleUseL2CAPEnabled.first(),
                        bleUseL2CAPInEngagement = bleUseL2CAPInEngagementEnabled.first()
                    )
                )
                Napier.d("advertisedTransports = $advertisedTransports", tag = TAG)

                val deviceEngagement = buildDeviceEngagement(
                    eDeviceKey = ephemeralDeviceKey.publicKey,
                    version = MdocConstants.VERSION
                ) {
                    connectionMethods.forEach(this::addConnectionMethod)
                }
                encodedDeviceEngagement = ByteString(Cbor.encode(deviceEngagement.toDataItem()))

                _qrCode.value = encodedDeviceEngagement
                setState(HolderState.ShowQrCode)

                // Then wait for connection
                val transport = advertisedTransports.waitForConnection(
                    eSenderKey = ephemeralDeviceKey.publicKey
                )

                model.setMechanism(
                    MdocPresentmentMechanism(
                        transport = transport,
                        ephemeralDeviceKey = ephemeralDeviceKey,
                        encodedDeviceEngagement = encodedDeviceEngagement,
                        handover = Simple.Companion.NULL,
                        engagementDuration = null,
                        allowMultipleRequests = false
                    )
                )
                setState(HolderState.Finished)
                _qrCode.value = null
                completionHandler(null)
            } catch (throwable: Throwable) {
                completionHandler(throwable)
            }
        }
    }
}
