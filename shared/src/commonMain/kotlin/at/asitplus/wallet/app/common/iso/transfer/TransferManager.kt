package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_nfc_mdoc_reader
import at.asitplus.wallet.app.common.presentation.TransferSettings.Companion.transferSettings
import data.document.RequestDocument
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.bytestring.ByteString
import org.jetbrains.compose.resources.getString
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.engagement.EngagementParser
import org.multipaz.mdoc.nfc.scanNfcMdocReader
import org.multipaz.mdoc.request.DeviceRequestGenerator
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportClosedException
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.util.Constants
import org.multipaz.util.UUID
import org.multipaz.util.fromBase64Url

// based on identity-credential[https://github.com/openwallet-foundation-labs/identity-credential] implementation
class TransferManager(
    private val scope: CoroutineScope,
    private val readerCertificateManager: ReaderCertificateManager,
    private val updateProgress: (String) -> Unit
) {

    enum class State {
        IDLE,
        RUNNING,
        DATA_RECEIVED,
        ERROR
    }
    private val _state = MutableStateFlow(State.IDLE)
    val state = _state.asStateFlow()

    var readerMostRecentDeviceResponse = mutableStateOf<ByteArray?>(null)
    var readerSessionTranscript: ByteArray? = null

    data class ConnectionMethodPickerData(
        val showPicker: Boolean,
        val connectionMethods: List<MdocConnectionMethod>,
        val continuation: CancellableContinuation<MdocConnectionMethod?>,
    )

    private suspend fun selectConnectionMethod(
        connectionMethods: List<MdocConnectionMethod>,
        connectionMethodPickerData: MutableState<ConnectionMethodPickerData?>
    ): MdocConnectionMethod? {
        return suspendCancellableCoroutine { continuation ->
            connectionMethodPickerData.value = ConnectionMethodPickerData(
                showPicker = true,
                connectionMethods = connectionMethods,
                continuation = continuation
            )
        }
    }

    private val connectionMethodPickerData = mutableStateOf<ConnectionMethodPickerData?>(null)
    private var readerTransport = mutableStateOf<MdocTransport?>(null)
    private var readerSessionEncryption = mutableStateOf<SessionEncryption?>(null)

    fun startNfcEngagement(
        documentRequest: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) {
        readerMostRecentDeviceResponse.value = null
        scope.launch {
            _state.value = State.RUNNING
            try {
                val negotiatedHandoverConnectionMethods = createConnectionMethods()
                scanNfcMdocReader(
                    message = getString(Res.string.info_text_nfc_mdoc_reader),
                    options = MdocTransportOptions(
                        bleUseL2CAP = transferSettings.readerBleL2CapEnabled.value
                    ),
                    selectConnectionMethod = { connectionMethods ->
                        if (transferSettings.readerAutomaticallySelectTransport) {
                            updateProgress("Auto-selected first from $connectionMethods")
                            connectionMethods[0]
                        } else {
                            selectConnectionMethod(connectionMethods, connectionMethodPickerData)
                        }
                    },
                    negotiatedHandoverConnectionMethods = negotiatedHandoverConnectionMethods,
                    onHandover = { transport, encodedDeviceEngagement, handover, updateMessage ->
                        doReaderFlow(
                            encodedDeviceEngagement = encodedDeviceEngagement,
                            handover = handover,
                            updateNfcDialogMessage = updateMessage,
                            selectConnectionMethod = { connectionMethods ->
                                if (transferSettings.readerAutomaticallySelectTransport) {
                                    updateProgress("Auto-selected first from $connectionMethods")
                                    connectionMethods[0]
                                } else {
                                    selectConnectionMethod(connectionMethods, connectionMethodPickerData)
                                }
                            },
                            documentRequest = documentRequest,
                            setDeviceResponseBytes = setDeviceResponseBytes
                        )
                    }
                )
            } catch (e: Throwable) {
                _state.value = State.ERROR
                // TODO: Add populate error to verifier
                Napier.e("NFC engagement failed", e)
                updateProgress("NFC engagement failed with $e")
            }
        }
    }

    private fun createConnectionMethods(): List<MdocConnectionMethod> {
        val connectionMethods = mutableListOf<MdocConnectionMethod>()
        val bleUuid = UUID.randomUUID()
        if (transferSettings.presentmentBleCentralClientModeEnabled.value) {
            connectionMethods.add(
                MdocConnectionMethodBle(
                    supportsPeripheralServerMode = false,
                    supportsCentralClientMode = true,
                    peripheralServerModeUuid = null,
                    centralClientModeUuid = bleUuid
                )
            )
        }
        if (transferSettings.presentmentBlePeripheralServerModeEnabled.value) {
            connectionMethods.add(
                MdocConnectionMethodBle(
                    supportsPeripheralServerMode = true,
                    supportsCentralClientMode = false,
                    peripheralServerModeUuid = bleUuid,
                    centralClientModeUuid = null
                )
            )
        }
        if (transferSettings.presentmentNfcDataTransferEnabled.value) {
            connectionMethods.add(
                MdocConnectionMethodNfc(
                    commandDataFieldMaxLength = 0xffff,
                    responseDataFieldMaxLength = 0x10000
                )
            )
        }
        return connectionMethods
    }

    private fun generateEncodedSessionTranscript(
        encodedDeviceEngagement: ByteArray,
        handover: DataItem,
        eReaderKey: EcPublicKey
    ): ByteArray {
        val encodedEReaderKey = Cbor.encode(eReaderKey.toCoseKey().toDataItem())
        return Cbor.encode(
            buildCborArray {
                add(Tagged(24, Bstr(encodedDeviceEngagement)))
                add(Tagged(24, Bstr(encodedEReaderKey)))
                add(handover)
            }
        )
    }

    fun doQrFlow(
        qrCode: String,
        documentRequest: RequestDocument,
        updateProgress: (String) -> Unit,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) = scope.launch {
        try {
            _state.value = State.RUNNING
            doReaderFlow(
                encodedDeviceEngagement = ByteString(qrCode.fromBase64Url()),
                handover = Simple.NULL,
                updateNfcDialogMessage = updateProgress,
                selectConnectionMethod = { connectionMethods ->
                    if (transferSettings.readerAutomaticallySelectTransport) {
                        Napier.d("Auto-selected first from $connectionMethods")
                        connectionMethods[0]
                    } else {
                        selectConnectionMethod(connectionMethods, connectionMethodPickerData)
                    }
                },
                documentRequest = documentRequest,
                setDeviceResponseBytes = setDeviceResponseBytes
            )
        } catch (error: Throwable) {
            _state.value = State.ERROR
            // TODO: handle error
            Napier.e("Caught exception", error)
            updateProgress("Error: ${error.message}")
        }
    }

    private suspend fun doReaderFlow(
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        updateNfcDialogMessage: ((message: String) -> Unit)?,
        selectConnectionMethod: suspend (connectionMethods: List<MdocConnectionMethod>) -> MdocConnectionMethod?,
        documentRequest: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) {
        val deviceEngagement = EngagementParser(encodedDeviceEngagement.toByteArray()).parse()
        val eDeviceKey = deviceEngagement.eSenderKey
        Napier.i("Using curve ${eDeviceKey.curve.name} for session encryption")
        val eReaderKey = Crypto.createEcPrivateKey(eDeviceKey.curve)

        readerTransport.value ?: run {
            val connectionMethods = MdocConnectionMethod.disambiguate(
                deviceEngagement.connectionMethods,
                MdocRole.MDOC_READER
            )
            val connectionMethod = if (connectionMethods.size == 1) {
                connectionMethods[0]
            } else {
                selectConnectionMethod(connectionMethods)
            }
            if (connectionMethod == null) {
                // If user canceled
                return@run null
            }
            MdocTransportFactory.Default.createTransport(
                connectionMethod,
                MdocRole.MDOC_READER,
                MdocTransportOptions(bleUseL2CAP = transferSettings.readerBleL2CapEnabled.value)
            )
        } ?: throw Throwable("No transport available")

        doReaderFlowWithTransport(
            encodedDeviceEngagement = encodedDeviceEngagement,
            handover = handover,
            updateNfcDialogMessage = updateNfcDialogMessage,
            eDeviceKey = eDeviceKey,
            eReaderKey = eReaderKey,
            documentRequest = documentRequest,
            setDeviceResponseBytes = setDeviceResponseBytes
        )
    }

    private suspend fun doReaderFlowWithTransport(
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        updateNfcDialogMessage: ((message: String) -> Unit)?,
        eDeviceKey: EcPublicKey,
        eReaderKey: EcPrivateKey,
        documentRequest: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) {
        if (updateNfcDialogMessage != null) {
            updateNfcDialogMessage("Transferring data, don't move your phone")
        }
        val encodedSessionTranscript = generateEncodedSessionTranscript(
            encodedDeviceEngagement.toByteArray(),
            handover,
            eReaderKey.publicKey
        )
        val sessionEncryption = SessionEncryption(
            MdocRole.MDOC_READER,
            eReaderKey,
            eDeviceKey,
            encodedSessionTranscript,
        )
        readerSessionEncryption.value = sessionEncryption
        this.readerSessionTranscript = encodedSessionTranscript
        val readerKey = readerCertificateManager.readerKey

        val encodedDeviceRequest =
            DeviceRequestGenerator(encodedSessionTranscript).addDocumentRequest(
                docType = documentRequest.docType,
                itemsToRequest = documentRequest.itemsToRequest,
                requestInfo = null,
                readerKey = readerKey,
                signatureAlgorithm = readerKey.curve.defaultSigningAlgorithm,
                readerKeyCertificateChain = X509CertChain(
                    listOf(
                        readerCertificateManager.readerCert,
                        readerCertificateManager.readerRootCert
                    )
                ),
            ).generate()

        try {
            val transport = readerTransport.value!!
            transport.open(eDeviceKey)
            transport.sendMessage(
                sessionEncryption.encryptMessage(
                    messagePlaintext = encodedDeviceRequest,
                    statusCode = null
                )
            )
            while (true) {
                Napier.d("Waiting for message")
                val sessionData = transport.waitForMessage()
                Napier.d("Got message")
                if (sessionData.isEmpty()) {
                    updateProgress("Received transport-specific session termination message from holder")
                    transport.close()
                    break
                }

                val (message, status) = sessionEncryption.decryptMessage(sessionData)
                Napier.i("Holder sent ${message?.size} bytes status $status")
                if (message != null) {
                    readerMostRecentDeviceResponse.value = message
                    setDeviceResponseBytes(message)
                    _state.value = State.DATA_RECEIVED
                }
                if (status == Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
                    updateProgress("Received session termination message from holder")
                    Napier.i(
                        "Holder indicated they closed the connection. Closing and ending reader loop"
                    )
                    transport.close()
                    break
                }
                if (!transferSettings.presentmentAllowMultipleRequests) {
                    updateProgress("Response received, closing connection")
                    Napier.i(
                        "Holder did not indicate they are closing the connection. " +
                                "Auto-close is enabled, so sending termination message, closing, and " +
                                "ending reader loop"
                    )
                    transport.sendMessage(SessionEncryption.encodeStatus(Constants.SESSION_DATA_STATUS_SESSION_TERMINATION))
                    transport.close()
                    break
                }
                updateProgress("Response received, keeping connection open")
                Napier.i(
                    "Holder did not indicate they are closing the connection. " +
                            "Auto-close is not enabled so waiting for message from holder"
                )
                // "Send additional request" and close buttons will act further on transport
            }
        } catch (_: MdocTransportClosedException) {
            // Nothing to do, this is thrown when at.asitplus.wallet.verifier.transport.close() is called from another coroutine, that
            // is, the onClick handlers for the close buttons.
            Napier.i("Ending reader flow due to MdocTransportClosedException")
        } finally {
            if (updateNfcDialogMessage != null) {
                updateNfcDialogMessage("Transfer complete")
            }
            readerTransport.value!!.close()
            readerTransport.value = null
            _state.value = State.IDLE
        }
    }
}
