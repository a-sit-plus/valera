package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import at.asitplus.KmmResult
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_nfc_mdoc_reader
import at.asitplus.wallet.app.common.data.SettingsRepository
import data.document.RequestDocumentList
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toDeprecatedInstant
import kotlinx.datetime.toInstant
import kotlinx.io.bytestring.ByteString
import org.jetbrains.compose.resources.getString
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
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
import org.multipaz.mdoc.transport.NfcTransportMdocReader
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.nfc.scanNfcTag
import org.multipaz.util.Constants
import org.multipaz.util.UUID
import org.multipaz.util.fromBase64Url

// based on identity-credential[https://github.com/openwallet-foundation-labs/identity-credential] implementation
class TransferManager(
    private val config: SettingsRepository,
    private val scope: CoroutineScope,
    private val updateProgress: (String) -> Unit,
) {

    // TODO: Add and update states to communicate with the verifier (connected, disconnected, error?)
    enum class State {
        IDLE,
        RUNNING,
        DATA_RECEIVED
    }

    var readerMostRecentDeviceResponse = mutableStateOf<ByteArray?>(null)
    var readerSessionTranscript: ByteArray? = null
    private val _state = MutableStateFlow(State.IDLE)

    /**
     * The current state.
     */
    val state = _state.asStateFlow()

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

    private val readerRootKey: EcPrivateKey by lazy {
        EcPrivateKey.fromPem(
            """
                    -----BEGIN PRIVATE KEY-----
                    MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgC42H+ZfAyMq4i3Na
                    bUrYsgtqflPxsgheWe8eygZ0Xl+gCgYIKoZIzj0DAQehRANCAAQmm+pmyUxx/x2e
                    D131E8HhvNkhsfYQXzefZlxgLXQPqCOxO+VPOXVOKL0dUy+kHyT5IP/NOAh038co
                    AVOgGPT4
                    -----END PRIVATE KEY-----
                """.trimIndent().trim(),
            EcPublicKey.fromPem(
                """
                    -----BEGIN PUBLIC KEY-----
                    MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEJpvqZslMcf8dng9d9RPB4bzZIbH2
                    EF83n2ZcYC10D6gjsTvlTzl1Tii9HVMvpB8k+SD/zTgIdN/HKAFToBj0+A==
                    -----END PUBLIC KEY-----
                    """.trimIndent().trim(),
                EcCurve.P256
            )
        )
    }

    private val readerRootCert: X509Cert by lazy {
        X509Cert.fromPem(
            """
                -----BEGIN CERTIFICATE-----
                MIICJzCCAc6gAwIBAgIUSvMftn/oM3etHjE7hdIBl6tWMV8wCgYIKoZIzj0EAwIw
                MzELMAkGA1UEBhMCQVQxDjAMBgNVBAoMBUEtU0lUMRQwEgYDVQQDDAtWYWxlcmEg
                SUFDQTAeFw0yNTA2MjYwODI0MDJaFw0yNjA2MjYwODI0MDJaMDMxCzAJBgNVBAYT
                AkFUMQ4wDAYDVQQKDAVBLVNJVDEUMBIGA1UEAwwLVmFsZXJhIElBQ0EwWTATBgcq
                hkjOPQIBBggqhkjOPQMBBwNCAAQmm+pmyUxx/x2eD131E8HhvNkhsfYQXzefZlxg
                LXQPqCOxO+VPOXVOKL0dUy+kHyT5IP/NOAh038coAVOgGPT4o4G/MIG8MBIGA1Ud
                EwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMCIGA1UdEgQbMBmGF2h0dHBz
                Oi8vd2FsbGV0LmEtc2l0LmF0MDIGA1UdHwQrMCkwJ6AloCOGIWh0dHBzOi8vd2Fs
                bGV0LmEtc2l0LmF0L2NybC8xLmNybDAfBgNVHSMEGDAWgBSDGoj0XuXE3qEVTmPv
                KSvIvR36ijAdBgNVHQ4EFgQUgxqI9F7lxN6hFU5j7ykryL0d+oowCgYIKoZIzj0E
                AwIDRwAwRAIgS9XcYA4Be5gDIdHmMOgJ3AeS44gT4bgVgsg/D5+WXS8CIAxJgi3n
                hGrVMj9SszehLorR2rR5FO5RZgITAaOIGSNP
                -----END CERTIFICATE-----
            """.trimIndent().trim()
        )
    }

    private val readerKey: EcPrivateKey = Crypto.createEcPrivateKey(EcCurve.P256)
    private val readerCert: X509Cert = MdocUtil.generateReaderCertificate(
        readerRootCert = readerRootCert,
        readerRootKey = readerRootKey,
        readerKey = readerKey.publicKey,
        subject = X500Name.fromName("CN=Valera Reader Cert"),
        serial = ASN1Integer(1L),
        validFrom = LocalDate.parse("2025-06-26").atTime(10, 0).toInstant(TimeZone.UTC).toDeprecatedInstant(),
        validUntil = LocalDate.parse("2026-06-26").atStartOfDayIn(TimeZone.UTC).toDeprecatedInstant(),
    )

    fun startNfcEngagement(
        documentRequestList: RequestDocumentList,
        setDeviceResponseBytes: (KmmResult<ByteArray>) -> Unit
    ) {
        readerMostRecentDeviceResponse.value = null

        scope.launch {
            try {
                val negotiatedHandoverConnectionMethods = mutableListOf<MdocConnectionMethod>()
                val bleUuid = UUID.randomUUID()
                if (config.presentmentBleCentralClientModeEnabled.first()) {
                    negotiatedHandoverConnectionMethods.add(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = false,
                            supportsCentralClientMode = true,
                            peripheralServerModeUuid = null,
                            centralClientModeUuid = bleUuid,
                        )
                    )
                }
                if (config.presentmentBlePeripheralServerModeEnabled.first()) {
                    negotiatedHandoverConnectionMethods.add(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = true,
                            supportsCentralClientMode = false,
                            peripheralServerModeUuid = bleUuid,
                            centralClientModeUuid = null,
                        )
                    )
                }
                if (config.presentmentNfcDataTransferEnabled.first()) {
                    negotiatedHandoverConnectionMethods.add(
                        MdocConnectionMethodNfc(
                            commandDataFieldMaxLength = 0xffff,
                            responseDataFieldMaxLength = 0x10000
                        )
                    )
                }

                scanNfcMdocReader(
                    message = getString(Res.string.info_text_nfc_mdoc_reader),
                    options = MdocTransportOptions(
                        bleUseL2CAP = config.readerBleL2CapEnabled.first()
                    ),
                    selectConnectionMethod = { connectionMethods ->
                        if (config.readerAutomaticallySelectTransport.first()) {
                            updateProgress("Auto-selected first from $connectionMethods")
                            connectionMethods[0]
                        } else {
                            selectConnectionMethod(
                                connectionMethods,
                                connectionMethodPickerData
                            )
                        }
                    },
                    negotiatedHandoverConnectionMethods = negotiatedHandoverConnectionMethods,
                    onHandover = { transport, encodedDeviceEngagement, handover, updateMessage ->
                        doReaderFlow(
                            encodedDeviceEngagement = encodedDeviceEngagement,
                            existingTransport = transport,
                            handover = handover,
                            updateNfcDialogMessage = updateMessage,
                            selectConnectionMethod = { connectionMethods ->
                                if (config.readerAutomaticallySelectTransport.first()) {
                                    updateProgress("Auto-selected first from $connectionMethods")
                                    connectionMethods[0]
                                } else {
                                    selectConnectionMethod(
                                        connectionMethods,
                                        connectionMethodPickerData
                                    )
                                }
                            },
                            documentRequestList = documentRequestList,
                            setDeviceResponseBytes = setDeviceResponseBytes
                        )
                    }
                )
            } catch (e: Throwable) {
                // TODO: Add populate error to verifier
                Napier.e("NFC engagement failed", e)
                updateProgress("NFC engagement failed with $e")
                setDeviceResponseBytes(KmmResult.failure(e))
            }
        }
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
        documentRequestList: RequestDocumentList,
        updateProgress: (String) -> Unit,
        setDeviceResponseBytes: (KmmResult<ByteArray>) -> Unit
    ) = scope.launch {
        try {
            doReaderFlow(
                encodedDeviceEngagement = ByteString(qrCode.fromBase64Url()),
                existingTransport = null,
                handover = Simple.NULL,
                updateNfcDialogMessage = updateProgress,
                selectConnectionMethod = { connectionMethods ->
                    if (config.readerAutomaticallySelectTransport.first()) {
                        updateProgress("Auto-selected first from $connectionMethods")
                        connectionMethods[0]
                    } else {
                        selectConnectionMethod(
                            connectionMethods,
                            connectionMethodPickerData
                        )
                    }
                },
                documentRequestList = documentRequestList,
                setDeviceResponseBytes = setDeviceResponseBytes
            )
        } catch (error: Throwable) {
            Napier.e("Caught exception", error)
            updateProgress("Error: ${error.message}")
            setDeviceResponseBytes(KmmResult.failure(error))
        }
    }

    private suspend fun doReaderFlow(
        encodedDeviceEngagement: ByteString,
        existingTransport: MdocTransport?,
        handover: DataItem,
        updateNfcDialogMessage: ((message: String) -> Unit)?,
        selectConnectionMethod: suspend (connectionMethods: List<MdocConnectionMethod>) -> MdocConnectionMethod?,
        documentRequestList: RequestDocumentList,
        setDeviceResponseBytes: (KmmResult<ByteArray>) -> Unit
    ) {
        val deviceEngagement = EngagementParser(encodedDeviceEngagement.toByteArray()).parse()
        val eDeviceKey = deviceEngagement.eSenderKey
        Napier.i("Using curve ${eDeviceKey.curve.name} for session encryption")
        val eReaderKey = Crypto.createEcPrivateKey(eDeviceKey.curve)

        val transport = if (existingTransport != null) {
            existingTransport
        } else {
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
                return
            }
            val transport = MdocTransportFactory.Default.createTransport(
                connectionMethod,
                MdocRole.MDOC_READER,
                MdocTransportOptions(bleUseL2CAP = config.readerBleL2CapEnabled.first())
            )
            if (transport is NfcTransportMdocReader) {
                scanNfcTag(
                    message = "QR engagement with NFC Data Transfer. Move into NFC field of the mdoc",
                    tagInteractionFunc = { tag, _ ->
                        transport.setTag(tag)
                        doReaderFlowWithTransport(
                            transport = transport,
                            encodedDeviceEngagement = encodedDeviceEngagement,
                            handover = handover,
                            updateNfcDialogMessage = updateNfcDialogMessage,
                            eDeviceKey = eDeviceKey,
                            eReaderKey = eReaderKey,
                            documentRequestList = documentRequestList,
                            setDeviceResponseBytes = setDeviceResponseBytes
                        )
                    }
                )
                return
            }
            transport
        }
        doReaderFlowWithTransport(
            transport = transport,
            encodedDeviceEngagement = encodedDeviceEngagement,
            handover = handover,
            updateNfcDialogMessage = updateNfcDialogMessage,
            eDeviceKey = eDeviceKey,
            eReaderKey = eReaderKey,
            documentRequestList = documentRequestList,
            setDeviceResponseBytes = setDeviceResponseBytes
        )
    }

    private suspend fun doReaderFlowWithTransport(
        transport: MdocTransport,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        updateNfcDialogMessage: ((message: String) -> Unit)?,
        eDeviceKey: EcPublicKey,
        eReaderKey: EcPrivateKey,
        documentRequestList: RequestDocumentList,
        setDeviceResponseBytes: (KmmResult<ByteArray>) -> Unit
    ) {
        if (updateNfcDialogMessage != null) {
            updateNfcDialogMessage("Transferring data, don't move your phone")
        }
        readerTransport.value = transport
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

        val generator = DeviceRequestGenerator(encodedSessionTranscript)
        documentRequestList.getAll().forEach { requestDocument ->
            generator.addDocumentRequest(
                docType = requestDocument.docType,
                itemsToRequest = requestDocument.itemsToRequest,
                requestInfo = null,
                readerKey = readerKey,
                signatureAlgorithm = readerKey.curve.defaultSigningAlgorithm,
                readerKeyCertificateChain = X509CertChain(listOf(readerCert, readerRootCert))
            )
        }
        val encodedDeviceRequest = generator.generate()

        try {
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
                    setDeviceResponseBytes(KmmResult.success(message))
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
                if (!config.presentmentAllowMultipleRequests.first()) {
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
            transport.close()
            readerTransport.value = null
        }
    }

}