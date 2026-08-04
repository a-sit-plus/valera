package at.asitplus.wallet.app.common.presentation

import at.asitplus.dcapi.NFCHandover
import at.asitplus.iso.SessionTranscript
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Simple
import org.multipaz.crypto.EcPublicKey
import org.multipaz.mdoc.request.DeviceRequest
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransport.State
import org.multipaz.mdoc.transport.MdocTransportClosedException
import org.multipaz.mdoc.transport.NfcTransportMdoc
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

        // Monitor transport failure in parallel. If the transport drops while runProcess is
        // suspended waiting for user input (e.g. NFC disconnected during the consent dialog),
        // setCompleted signals the credential-selection continuation so the dialog closes.
        val transportMonitorJob = CoroutineScope(currentCoroutineContext()).launch {
            transport.state.first { it == State.FAILED || it == State.CLOSED }
            if (stateModel.state.value != PresentationStateModel.State.COMPLETED) {
                stateModel.setCompleted(Error("Connection was lost"))
            }
        }
        try {
            runProcess(dismissible, numRequestsServed, credentialSelected, transport)
        } catch (_: MdocTransportClosedException) {
            // Nothing to do, this is thrown when transport.close() is called from another coroutine, that
            // is, the X in the top-right
            Napier.i("Ending holderJob due to MdocTransportClosedException")
            stateModel.setCompleted()
        } catch (error: CancellationException) {
            if (stateModel.state.value == PresentationStateModel.State.COMPLETED
                || stateModel.state.value == PresentationStateModel.State.IDLE
            ) {
                Napier.i("Ending holderJob after presentment teardown")
            } else {
                throw error
            }
        } catch (error: Throwable) {
            Napier.e("MdocPresenter: Caught exception", error)
            stateModel.setCompleted(error)
        } finally {
            transportMonitorJob.cancel()
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
                sessionTranscript = calcSessionTranscript(eReaderKey.publicKey)
                encodedSessionTranscript = coseCompliantSerializer.encodeToByteArray(sessionTranscript)
                Napier.i(
                    "Holder initialized session encryption; handover=${mechanism.handover::class.simpleName}, " +
                            "sessionTranscriptBytes=${encodedSessionTranscript.size}",
                )
                sessionEncryption = SessionEncryption(
                    MdocRole.MDOC,
                    mechanism.ephemeralDeviceKey,
                    eReaderKey.publicKey,
                    encodedSessionTranscript,
                )
            }
            val (encodedDeviceRequest, status) = sessionEncryption.decryptMessage(sessionData)
            Napier.i(
                "Holder received reader message; requestBytes=${encodedDeviceRequest?.size}, status=$status"
            )

            if (status == Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
                Napier.i("mdocPresentment: Received session termination message from reader")
                stateModel.setCompleted()
                break
            }

            // TODO: check the reader authentication
            val deviceRequest = DeviceRequest.fromDataItem(Cbor.decode(encodedDeviceRequest!!))
//            deviceRequest.verifyReaderAuthentication(sessionTranscript = RawCbor(encodedSessionTranscript!!))

            presentationViewModel.initWithDeviceRequest(
                parsedRequest = coseCompliantSerializer.decodeFromByteArray(encodedDeviceRequest),
                finishFunction = credentialSelected,
                sessionTranscript = sessionTranscript
            )
            Napier.d("Waiting for credential selection from UI")
            // While the consent dialog is open the main loop is not calling waitForMessage(),
            // so a reader-sent SESSION_TERMINATION would sit unread in the transport queue.
            // This side job consumes that message and surfaces it immediately.
            val readerClosedJob = CoroutineScope(currentCoroutineContext()).launch {
                try {
                    val data = transport.waitForMessage()
                    if (stateModel.state.value != PresentationStateModel.State.COMPLETED) {
                        if (data.isEmpty()) {
                            stateModel.setCompleted(Error("Reader closed the connection"))
                        } else {
                            val (_, status) = sessionEncryption.decryptMessage(data)
                            if (status == Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
                                stateModel.setCompleted(Error("Reader closed the connection"))
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    // Normal cancellation when the user selects a credential.
                } catch (_: MdocTransportClosedException) {
                    if (stateModel.state.value != PresentationStateModel.State.COMPLETED) {
                        stateModel.setCompleted(Error("Reader closed the connection"))
                    }
                } catch (e: Throwable) {
                    if (stateModel.state.value != PresentationStateModel.State.COMPLETED) {
                        stateModel.setCompleted(e)
                    }
                }
            }
            val response = try {
                stateModel.requestCredentialSelection()
            } catch (error: PresentmentCanceled) {
                Napier.i("Holder presentation canceled by user; sending encrypted session termination to reader")
                withContext(NonCancellable) {
                    mechanism.transport.sendMessage(
                        sessionEncryption.encryptMessage(
                            messagePlaintext = null,
                            statusCode = Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
                        )
                    )
                    if (mechanism.transport is NfcTransportMdoc) {
                        delay(NFC_TRANSPORT_DRAIN_DELAY_MS)
                    }
                }
                stateModel.setCompleted(error)
                break
            } finally {
                readerClosedJob.cancel()
            }
            withContext(NonCancellable) {
                Napier.d("Credential selected, sending ${response.size} bytes to reader")
                mechanism.transport.sendMessage(response.encrypt(sessionEncryption))
                Napier.i("Holder DeviceResponse sent to reader; bytes=${response.size}")
            }

            numRequestsServed.value += 1
            if (!mechanism.allowMultipleRequests) {
                withContext(NonCancellable) {
                    // For the single-request local presentation flow we already send a session
                    // termination status together with the response. Completing immediately avoids
                    // hanging the UI on delayed or failed BLE close notifications.
                    if (mechanism.transport is NfcTransportMdoc) {
                        Napier.i("NFC response sent; allowing reader APDU exchange to drain before closing holder transport")
                        delay(NFC_TRANSPORT_DRAIN_DELAY_MS)
                    }
                    Napier.i("Response sent, completing single-request presentment")
                    stateModel.setCompleted()
                }
                break
            } else {
                Napier.i("Response sent, keeping connection open")
            }
        }
    }

    private suspend fun ByteArray.encrypt(
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

    private companion object {
        private const val NFC_TRANSPORT_DRAIN_DELAY_MS = 1_500L
    }
}
