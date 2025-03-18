package ui.viewmodels.iso

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import com.android.identity.cbor.Simple
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcCurve
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.connectionmethod.ConnectionMethodBle
import com.android.identity.mdoc.engagement.EngagementGenerator
import com.android.identity.mdoc.transport.MdocTransport
import com.android.identity.mdoc.transport.MdocTransportFactory
import com.android.identity.mdoc.transport.MdocTransportOptions
import com.android.identity.mdoc.transport.advertiseAndWait
import com.android.identity.util.UUID
import com.android.identity.util.toBase64Url
import io.github.aakira.napier.Napier
import kotlinx.io.bytestring.ByteString
import qrcode.QRCode
import ui.viewmodels.PresentationStateModel

class ShowQrCodeViewModel(
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val onNavigateToPresentmentScreen: (PresentationStateModel) -> Unit,
) {
    var permission by mutableStateOf(false)
    val presentationStateModel: PresentationStateModel by lazy { PresentationStateModel() }

    suspend fun doHolderFlow(showQrCode: MutableState<ByteString?>, ) {
        val ephemeralDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
        lateinit var encodedDeviceEngagement: ByteString

        val connectionMethods = mutableListOf<ConnectionMethod>()
        val bleUuid = UUID.randomUUID()
        connectionMethods.add(
            ConnectionMethodBle(
                supportsPeripheralServerMode = true,
                supportsCentralClientMode = false,
                peripheralServerModeUuid = bleUuid,
                centralClientModeUuid = null,
            )
        )

        val transport = connectionMethods.advertiseAndWait(
            role = MdocTransport.Role.MDOC,
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

    fun createQrCode(showQrCode: ByteString): ByteArray {
        Napier.d("DeviceEngagement: ${showQrCode.toByteArray()}")
        val deviceEngagementQrCode = "mdoc:" + showQrCode.toByteArray().toBase64Url()
        return QRCode.ofSquares()
            .build(deviceEngagementQrCode)
            .renderToBytes()
    }
}
