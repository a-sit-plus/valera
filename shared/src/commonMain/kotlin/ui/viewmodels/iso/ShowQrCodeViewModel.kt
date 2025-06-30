package ui.viewmodels.iso

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Simple
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.engagement.EngagementGenerator
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.advertise
import org.multipaz.mdoc.transport.waitForConnection
import org.multipaz.util.UUID
import ui.viewmodels.authentication.PresentationStateModel
import kotlin.lazy

class ShowQrCodeViewModel(
    val walletMain: WalletMain,
    val settingsRepository: SettingsRepository,
) : ViewModel() {
    var hasBeenCalledHack: Boolean = false

    val presentationScope by lazy { CoroutineScope(Dispatchers.IO + CoroutineName("QR code presentation scope") + walletMain.coroutineExceptionHandler) }
    val presentationStateModel by lazy { PresentationStateModel(presentationScope) }


    private val _showQrCodeState = MutableStateFlow(ShowQrCodeState.INIT)
    val showQrCodeState: StateFlow<ShowQrCodeState> = _showQrCodeState

    fun setState(newState: ShowQrCodeState) {
        _showQrCodeState.value = newState
    }

    fun setupPresentmentModel() {
        presentationStateModel.reset()
        presentationStateModel.init()
        presentationStateModel.start(needBluetooth = true)
        presentationStateModel.setPermissionState(true)
    }

    fun doHolderFlow(
        showQrCode: MutableState<ByteString?>,
        completionHandler: CompletionHandler = {}
    ) = presentationStateModel.presentmentScope.launch {
        try {
            val connectionMethods = mutableListOf<MdocConnectionMethod>()
            val bleUuid = UUID.randomUUID()

            if (settingsRepository.presentmentBleCentralClientModeEnabled.first()) {
                connectionMethods.add(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = false,
                        supportsCentralClientMode = true,
                        peripheralServerModeUuid = null,
                        centralClientModeUuid = bleUuid,
                    )
                )
            }
            if (settingsRepository.presentmentBlePeripheralServerModeEnabled.first()) {
                connectionMethods.add(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = true,
                        supportsCentralClientMode = false,
                        peripheralServerModeUuid = bleUuid,
                        centralClientModeUuid = null,
                    )
                )
            }
            if (settingsRepository.presentmentNfcDataTransferEnabled.first()) {
                connectionMethods.add(
                    MdocConnectionMethodNfc(
                        commandDataFieldMaxLength = 0xffff,
                        responseDataFieldMaxLength = 0x10000
                    )
                )
            }

            val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
            lateinit var encodedDeviceEngagement: ByteString

            // First advertise the connection methods
            val advertisedTransports = connectionMethods.advertise(
                role = MdocRole.MDOC,
                transportFactory = MdocTransportFactory.Default,
                options = MdocTransportOptions(
                    bleUseL2CAP = settingsRepository.readerBleL2CapEnabled.first()
                )
            )

            // Generate engagement
            val engagementGenerator = EngagementGenerator(
                eSenderKey = ephemeralDeviceKey.publicKey,
                version = MdocConstants.VERSION
            )
            engagementGenerator.addConnectionMethods(connectionMethods)
            val encodedDeviceEngagementByteArray = engagementGenerator.generate()
            encodedDeviceEngagement = ByteString(encodedDeviceEngagementByteArray)
            showQrCode.value = encodedDeviceEngagement
            setState(ShowQrCodeState.SHOW_QR_CODE)

            // Then wait for connection
            val transport = advertisedTransports.waitForConnection(
                eSenderKey = ephemeralDeviceKey.publicKey
            )

            presentationStateModel.setMechanism(
                MdocPresentmentMechanism(
                    transport = transport,
                    ephemeralDeviceKey = ephemeralDeviceKey,
                    encodedDeviceEngagement = encodedDeviceEngagement,
                    handover = Simple.NULL,
                    engagementDuration = null,
                    allowMultipleRequests = false
                )
            )
            setState(ShowQrCodeState.FINISHED)
            showQrCode.value = null
            completionHandler(null)
        } catch (throwable: Throwable) {
            completionHandler(throwable)
        }
    }
}

enum class ShowQrCodeState {
    INIT,
    NO_TRANSFER_METHOD_AVAILABLE,
    MISSING_PERMISSION,
    CREATE_ENGAGEMENT,
    SHOW_QR_CODE,
    FINISHED
}
