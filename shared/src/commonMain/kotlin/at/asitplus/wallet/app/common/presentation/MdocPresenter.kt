package at.asitplus.wallet.app.common.presentation

import at.asitplus.dcapi.NFCHandover
import at.asitplus.iso.SessionTranscript
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Simple
import org.multipaz.crypto.EcPublicKey
import org.multipaz.mdoc.request.DeviceRequestParser
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransport.State
import org.multipaz.mdoc.transport.MdocTransportClosedException
import org.multipaz.util.Constants
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.authentication.PresentationViewModel


// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp
class MdocPresenter(
    private val stateModel: PresentationStateModel,
    private val presentationViewModel: PresentationViewModel,
    private val mechanism: MdocPresentmentMechanism
) {
    internal suspend fun present(
        dismissible: MutableStateFlow<Boolean>,
        numRequestsServed: MutableStateFlow<Int>,
        credentialSelected: (ByteArray) -> Unit,
    ) {
        val transport = mechanism.transport
        // Wait until state changes to CONNECTED, FAILED, or CLOSED
        transport.state.first {
            it in listOf(State.CONNECTED, State.FAILED, State.CLOSED)
        }
        if (transport.state.value != State.CONNECTED) {
            stateModel.setCompleted(Error("Expected state CONNECTED but found ${transport.state.value}"))
            return
        }

        try {
            runProcess(dismissible, numRequestsServed, credentialSelected, transport)
        } catch (_: MdocTransportClosedException) {
            // Nothing to do, this is thrown when transport.close() is called from another coroutine, that
            // is, the X in the top-right
            Napier.i("Ending holderJob due to MdocTransportClosedException")
            stateModel.setCompleted()
        } catch (error: Throwable) {
            Napier.e("Caught exception", error)
            stateModel.setCompleted(error)
        }
        transport.close()
    }

    private suspend fun runProcess(
        dismissible: MutableStateFlow<Boolean>,
        numRequestsServed: MutableStateFlow<Int>,
        credentialSelected: (ByteArray) -> Unit,
        transport: MdocTransport
    ) {
        var sessionEncryption: SessionEncryption? = null
        var encodedSessionTranscript: ByteArray? = null
        var sessionTranscript: SessionTranscript? = null
        while (true) {
            Napier.i("Waiting for message from reader...")
            dismissible.value = true
            val sessionData = transport.waitForMessage()
            dismissible.value = false
            if (sessionData.isEmpty()) {
                Napier.i("Received transport-specific session termination message from reader")
                stateModel.setCompleted()
                break
            }

            if (sessionEncryption == null) {
                val eReaderKey = SessionEncryption.getEReaderKey(sessionData)
                sessionTranscript = calcSessionTranscript(eReaderKey)
                encodedSessionTranscript = coseCompliantSerializer.encodeToByteArray(sessionTranscript)
                sessionEncryption = SessionEncryption(
                    MdocRole.MDOC,
                    mechanism.ephemeralDeviceKey,
                    eReaderKey,
                    encodedSessionTranscript,
                )
            }
            val (encodedDeviceRequest, status) = sessionEncryption.decryptMessage(sessionData)

            if (status == Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
                Napier.i("mdocPresentment: Received session termination message from reader")
                stateModel.setCompleted()
                break
            }

            //TODO use our libs to check the reader authentication
            DeviceRequestParser(
                encodedDeviceRequest = encodedDeviceRequest!!,
                encodedSessionTranscript = encodedSessionTranscript!!,
            ).parse()

            presentationViewModel.initWithDeviceRequest(
                parsedRequest = coseCompliantSerializer.decodeFromByteArray(encodedDeviceRequest),
                finishFunction = credentialSelected,
                sessionTranscript = sessionTranscript
            )
            val response = stateModel.requestCredentialSelection()

            mechanism.transport.sendMessage(response.encrypt(sessionEncryption))

            numRequestsServed.value += 1
            if (!mechanism.allowMultipleRequests) {
                // Wait for transport to be closed as sendMessage does not block as advertised in its description
                transport.state.first {
                    it in listOf(State.CLOSED, State.FAILED)
                }
                Napier.i("Response sent, closing connection")
                stateModel.setCompleted()
                break
            } else {
                Napier.i("Response sent, keeping connection open")
            }
        }
    }

    private fun ByteArray.encrypt(
        sessionEncryption: SessionEncryption
    ): ByteArray = sessionEncryption.encryptMessage(
        messagePlaintext = this,
        statusCode = if (!mechanism.allowMultipleRequests) Constants.SESSION_DATA_STATUS_SESSION_TERMINATION else null
    )

    private fun calcSessionTranscript(eReaderKey: EcPublicKey): SessionTranscript =
        if (mechanism.handover == Simple.NULL) {
            SessionTranscript.forQr(
                deviceEngagementBytes = mechanism.encodedDeviceEngagement.toByteArray(),
                eReaderKeyBytes = eReaderKey.getBytes()
            )
        } else {
            val nfcHandover = coseCompliantSerializer.decodeFromByteArray<NFCHandover>(
                Cbor.encode(mechanism.handover)
            )
            SessionTranscript.forNfc(
                deviceEngagementBytes = mechanism.encodedDeviceEngagement.toByteArray(),
                eReaderKeyBytes = eReaderKey.getBytes(),
                nfcHandover = nfcHandover
            )
        }

    private fun EcPublicKey.getBytes(): ByteArray =
        coseCompliantSerializer.encodeToByteArray(
            coseCompliantSerializer.decodeFromByteArray<CoseKey>(
                Cbor.encode(toCoseKey().toDataItem())
            )
        )
}
