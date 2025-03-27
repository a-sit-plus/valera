package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.lib.iso.DeviceRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.mdoc.request.DeviceRequestParser
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportClosedException
import org.multipaz.mdoc.util.toMdocRequest
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
            it == MdocTransport.State.CONNECTED ||
                    it == MdocTransport.State.FAILED ||
                    it == MdocTransport.State.CLOSED
        }
        if (transport.state.value != MdocTransport.State.CONNECTED) {
            stateModel.setCompleted(Error("Expected state CONNECTED but found ${transport.state.value}"))
            return
        }

        try {
            var sessionEncryption: SessionEncryption? = null
            var encodedSessionTranscript: ByteArray? = null
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
                    encodedSessionTranscript = Cbor.encode(
                        buildCborArray {
                            add(Tagged(24, Bstr(mechanism.encodedDeviceEngagement.toByteArray())))
                            add(Tagged(24, Bstr(Cbor.encode(eReaderKey.toCoseKey().toDataItem()))))
                            add(mechanism.handover)
                        }
                    )
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

                val deviceRequest = DeviceRequestParser(
                    encodedDeviceRequest!!,
                    encodedSessionTranscript!!,
                ).parse()


                //TODO use our libs to parse the device request and check the reader authentication
                val mdocRequests = deviceRequest.docRequests.map {
                    it.toMdocRequest(
                        documentTypeRepository = DocumentTypeRepository(),
                        mdocCredential = null
                    )
                }

                presentationViewModel.initWithMdocRequest(mdocRequests, credentialSelected)


                val response = stateModel.requestCredentialSelection()

                mechanism.transport.sendMessage(
                    sessionEncryption.encryptMessage(
                        response,
                        if (!mechanism.allowMultipleRequests) {
                            Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
                        } else {
                            null
                        }
                    )
                )

                numRequestsServed.value += 1
                if (!mechanism.allowMultipleRequests) {
                    Napier.i("Response sent, closing connection")
                    stateModel.setCompleted()
                    break
                } else {
                    Napier.i("Response sent, keeping connection open")
                }
            }
        } catch (_: MdocTransportClosedException) {
            // Nothing to do, this is thrown when transport.close() is called from another coroutine, that
            // is, the X in the top-right
            Napier.i("Ending holderJob due to MdocTransportClosedException")
            stateModel.setCompleted()
        } catch (error: Throwable) {
            Napier.e("Caught exception", error)
            stateModel.setCompleted(error)
        }
    }
}
