package ui.viewmodels.iso.holder

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants
import at.asitplus.wallet.app.common.iso.transfer.state.HolderState
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
    settingsRepository: SettingsRepository
) : TransferOptionsViewModel(walletMain, settingsRepository) {
    private val _qrCode = MutableStateFlow<ByteString?>(null)
    val qrCode: StateFlow<ByteString?> = _qrCode.asStateFlow()

    val onResume: () -> Unit = { setState(HolderState.Settings) }
    val onConsentSettings: () -> Unit = { setState(HolderState.CheckSettings) }
    var hasBeenCalledHack: Boolean = false

    val presentationScope by lazy {
        CoroutineScope(
            Dispatchers.IO + CoroutineName("QR code presentation scope") +
                    walletMain.coroutineExceptionHandler
        )
    }
    val presentationStateModel by lazy {
        PresentationStateModel(presentationScope)
    }

    private val _holderState = MutableStateFlow<HolderState>(HolderState.Settings)
    val holderState: StateFlow<HolderState> = _holderState

    fun setState(newState: HolderState) {
        if (_holderState.value == newState) return
        Napier.d("Change state from ${_holderState.value} to $newState", tag = "HolderViewModel")
        _holderState.value = newState
    }

    fun setupPresentmentModel(
        blePermissionState: PermissionState,
        isBluetoothRequired: Boolean
    ) {
        presentationStateModel.reset()
        presentationStateModel.init()
        presentationStateModel.start(isBluetoothRequired)
        if (isBluetoothRequired) {
            presentationStateModel.setPermissionState(blePermissionState.isGranted)
        }
    }

    fun doHolderFlow(
        isBleEnabled: Boolean,
        isNfcEnabled: Boolean,
        completionHandler: CompletionHandler = {}
    ) = presentationStateModel.presentmentScope.launch {
        try {
            val connectionMethods = mutableListOf<MdocConnectionMethod>()
            val bleUuid = UUID.Companion.randomUUID()

            if (isBleEnabled) {
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

            if (isNfcEnabled) {
                if (presentmentNfcDataTransferEnabled.first()) {
                    connectionMethods.add(
                        MdocConnectionMethodNfc(
                            commandDataFieldMaxLength = 0xffff,
                            responseDataFieldMaxLength = 0x10000
                        )
                    )
                }
            }

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

            val deviceEngagement = buildDeviceEngagement(
                eDeviceKey = ephemeralDeviceKey.publicKey,
                version = MdocConstants.VERSION
            ) {
                connectionMethods.forEach(::addConnectionMethod)
            }
            encodedDeviceEngagement = ByteString(Cbor.encode(deviceEngagement.toDataItem()))

            _qrCode.value = encodedDeviceEngagement
            setState(HolderState.ShowQrCode)

            // Then wait for connection
            val transport = advertisedTransports.waitForConnection(
                eSenderKey = ephemeralDeviceKey.publicKey
            )

            presentationStateModel.setMechanism(
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