package ui.viewmodels.iso

import androidx.compose.runtime.MutableState
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Simple
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.engagement.EngagementGenerator
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.advertiseAndWait
import org.multipaz.util.UUID
import ui.viewmodels.authentication.PresentationStateModel

class ShowQrCodeViewModel(
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val onNavigateToPresentmentScreen: (PresentationStateModel) -> Unit,
) {
    val presentationStateModel: PresentationStateModel by lazy { PresentationStateModel() }

    suspend fun doHolderFlow(showQrCode: MutableState<ByteString?>, ) {
        val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
        lateinit var encodedDeviceEngagement: ByteString

        val connectionMethods = mutableListOf<MdocConnectionMethod>()
        val bleUuid = UUID.randomUUID()
        connectionMethods.add(
            MdocConnectionMethodBle(
                supportsPeripheralServerMode = true,
                supportsCentralClientMode = false,
                peripheralServerModeUuid = bleUuid,
                centralClientModeUuid = null,
            )
        )

        val transport = connectionMethods.advertiseAndWait(
            role = MdocRole.MDOC,
            transportFactory = MdocTransportFactory.Default,
            options = MdocTransportOptions(true),
            eSenderKey = ephemeralDeviceKey.publicKey,
            onConnectionMethodsReady = { advertisedConnectionMethods ->
                val engagementGenerator = EngagementGenerator(
                    eSenderKey = ephemeralDeviceKey.publicKey,
                    version = "1.0"
                )
                engagementGenerator.addConnectionMethods(advertisedConnectionMethods)
                val encodedDeviceEngagementByteArray = engagementGenerator.generate()
                encodedDeviceEngagement = ByteString(encodedDeviceEngagementByteArray)
                showQrCode.value = encodedDeviceEngagement
            }
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
        showQrCode.value = null
        onNavigateToPresentmentScreen(presentationStateModel)
    }
}
