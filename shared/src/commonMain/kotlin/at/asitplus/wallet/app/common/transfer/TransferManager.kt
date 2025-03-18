package at.asitplus.wallet.app.common.transfer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.android.identity.asn1.ASN1Integer
import com.android.identity.cbor.Bstr
import com.android.identity.cbor.Cbor
import com.android.identity.cbor.CborArray
import com.android.identity.cbor.DataItem
import com.android.identity.cbor.Simple
import com.android.identity.cbor.Tagged
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcCurve
import com.android.identity.crypto.EcPrivateKey
import com.android.identity.crypto.EcPublicKey
import com.android.identity.crypto.X500Name
import com.android.identity.crypto.X509Cert
import com.android.identity.crypto.X509CertChain
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.connectionmethod.ConnectionMethodNfc
import com.android.identity.mdoc.engagement.EngagementParser
import com.android.identity.mdoc.nfc.scanNfcMdocReader
import com.android.identity.mdoc.sessionencryption.SessionEncryption
import com.android.identity.mdoc.transport.MdocTransport
import com.android.identity.mdoc.transport.MdocTransportClosedException
import com.android.identity.mdoc.transport.MdocTransportFactory
import com.android.identity.mdoc.transport.MdocTransportOptions
import com.android.identity.mdoc.transport.NfcTransportMdocReader
import com.android.identity.mdoc.util.MdocUtil
import com.android.identity.nfc.scanNfcTag
import com.android.identity.util.Constants
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import com.android.identity.mdoc.connectionmethod.ConnectionMethodBle
import com.android.identity.mdoc.request.DeviceRequestGenerator
import com.android.identity.util.UUID
import com.android.identity.util.fromBase64Url
import data.document.RequestDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.bytestring.ByteString

// based on identity-credential[https://github.com/openwallet-foundation-labs/identity-credential] implementation
class TransferManager(private val scope: CoroutineScope) {
    private val tag = "TransferManager"
    
    enum class State {
        IDLE,
        RUNNING,
        DATA_RECEIVED
    }

    private val transferSettings = TransferSettings()
    var readerMostRecentDeviceResponse =  mutableStateOf<ByteArray?>(null)
    var readerSessionTranscript: ByteArray? = null
    private val _state = MutableStateFlow(State.IDLE)
    /**
     * The current state.
     */
    val state = _state.asStateFlow()

    data class ConnectionMethodPickerData(
        val showPicker: Boolean,
        val connectionMethods: List<ConnectionMethod>,
        val continuation: CancellableContinuation<ConnectionMethod?>,
    )

    private suspend fun selectConnectionMethod(
        connectionMethods: List<ConnectionMethod>,
        connectionMethodPickerData: MutableState<ConnectionMethodPickerData?>
    ): ConnectionMethod? {
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


    // TODO public keys taken from identity-credential, replace with our own
    private val bundledReaderRootKey: EcPrivateKey by lazy {
        val readerRootKeyPub = EcPublicKey.fromPem(
            """
                    -----BEGIN PUBLIC KEY-----
                    MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE+QDye70m2O0llPXMjVjxVZz3m5k6agT+
                    wih+L79b7jyqUl99sbeUnpxaLD+cmB3HK3twkA7fmVJSobBc+9CDhkh3mx6n+YoH
                    5RulaSWThWBfMyRjsfVODkosHLCDnbPV
                    -----END PUBLIC KEY-----
                """.trimIndent().trim(),
            EcCurve.P384
        )
        EcPrivateKey.fromPem(
            """
                    -----BEGIN PRIVATE KEY-----
                    MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCcRuzXW3pW2h9W8pu5
                    /CSR6JSnfnZVATq+408WPoNC3LzXqJEQSMzPsI9U1q+wZ2yhZANiAAT5APJ7vSbY
                    7SWU9cyNWPFVnPebmTpqBP7CKH4vv1vuPKpSX32xt5SenFosP5yYHccre3CQDt+Z
                    UlKhsFz70IOGSHebHqf5igflG6VpJZOFYF8zJGOx9U4OSiwcsIOds9U=
                    -----END PRIVATE KEY-----
                """.trimIndent().trim(),
            readerRootKeyPub
        )
    }

    private val bundledReaderRootCert: X509Cert by lazy {
        MdocUtil.generateReaderRootCertificate(
            readerRootKey = iacaKey,
            subject = X500Name.fromName("CN=OWF IC TestApp Reader Root"),
            serial = ASN1Integer(1L),
            validFrom = certsValidFrom,
            validUntil = certsValidUntil,
        )
    }
    private val bundledIacaKey: EcPrivateKey by lazy {
        val iacaKeyPub = EcPublicKey.fromPem(
            """
                    -----BEGIN PUBLIC KEY-----
                    MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE+QDye70m2O0llPXMjVjxVZz3m5k6agT+
                    wih+L79b7jyqUl99sbeUnpxaLD+cmB3HK3twkA7fmVJSobBc+9CDhkh3mx6n+YoH
                    5RulaSWThWBfMyRjsfVODkosHLCDnbPV
                    -----END PUBLIC KEY-----
                """.trimIndent().trim(),
            EcCurve.P384
        )
        EcPrivateKey.fromPem(
            """
                    -----BEGIN PRIVATE KEY-----
                    MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCcRuzXW3pW2h9W8pu5
                    /CSR6JSnfnZVATq+408WPoNC3LzXqJEQSMzPsI9U1q+wZ2yhZANiAAT5APJ7vSbY
                    7SWU9cyNWPFVnPebmTpqBP7CKH4vv1vuPKpSX32xt5SenFosP5yYHccre3CQDt+Z
                    UlKhsFz70IOGSHebHqf5igflG6VpJZOFYF8zJGOx9U4OSiwcsIOds9U=
                    -----END PRIVATE KEY-----
                """.trimIndent().trim(),
            iacaKeyPub
        )
    }

    val bundledIacaCert: X509Cert by lazy {
        MdocUtil.generateIacaCertificate(
            iacaKey = iacaKey,
            subject = X500Name.fromName("C=ZZ,CN=OWF Identity Credential TEST IACA"),
            serial = ASN1Integer(1L),
            validFrom = certsValidFrom,
            validUntil = certsValidUntil,
            issuerAltNameUrl = "https://github.com/openwallet-foundation-labs/identity-credential",
            crlUrl = "https://github.com/openwallet-foundation-labs/identity-credential"
        )
    }

    private val certsValidFrom = LocalDate.parse("2024-12-01").atStartOfDayIn(TimeZone.UTC)
    private val certsValidUntil = LocalDate.parse("2034-12-01").atStartOfDayIn(TimeZone.UTC)
    private val iacaKey: EcPrivateKey = bundledIacaKey
    private val readerRootKey: EcPrivateKey = bundledReaderRootKey
    private val readerRootCert: X509Cert = bundledReaderRootCert
    private val readerKey: EcPrivateKey = Crypto.createEcPrivateKey(EcCurve.P256)
    private val readerCert: X509Cert = MdocUtil.generateReaderCertificate(
        readerRootCert = readerRootCert,
        readerRootKey = readerRootKey,
        readerKey = readerKey.publicKey,
        subject = X500Name.fromName("CN=OWF IC TestApp Reader Cert"),
        serial = ASN1Integer(1L),
        validFrom = certsValidFrom,
        validUntil = certsValidUntil,
    )
    private val iacaCert: X509Cert = bundledIacaCert

    fun startNfcEngagement(
        requestDocument: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) {
        readerMostRecentDeviceResponse.value = null

        scope.launch {
            try {
                val negotiatedHandoverConnectionMethods = mutableListOf<ConnectionMethod>()
                val bleUuid = UUID.randomUUID()
                if (transferSettings.presentmentBleCentralClientModeEnabled) {
                    negotiatedHandoverConnectionMethods.add(
                        ConnectionMethodBle(
                            supportsPeripheralServerMode = false,
                            supportsCentralClientMode = true,
                            peripheralServerModeUuid = null,
                            centralClientModeUuid = bleUuid,
                        )
                    )
                }
                if (transferSettings.presentmentBlePeripheralServerModeEnabled) {
                    negotiatedHandoverConnectionMethods.add(
                        ConnectionMethodBle(
                            supportsPeripheralServerMode = true,
                            supportsCentralClientMode = false,
                            peripheralServerModeUuid = bleUuid,
                            centralClientModeUuid = null,
                        )
                    )
                }
                if (transferSettings.presentmentNfcDataTransferEnabled) {
                negotiatedHandoverConnectionMethods.add(
                    ConnectionMethodNfc(
                        commandDataFieldMaxLength = 0xffff,
                        responseDataFieldMaxLength = 0x10000
                    )
                )
                }
                scanNfcMdocReader(
                    message = "Hold near credential holder's phone.",
                    options = MdocTransportOptions(
                        bleUseL2CAP = transferSettings.readerBleL2CapEnabled
                    ),
                    selectConnectionMethod = { connectionMethods ->
                        if (transferSettings.readerAutomaticallySelectTransport) {
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
                        Napier.d("NFC Engagement: onHandover: $updateMessage", tag = tag)
                        doReaderFlow(
                            encodedDeviceEngagement = encodedDeviceEngagement,
                            existingTransport = transport,
                            handover = handover,
                            selectConnectionMethod = { connectionMethods ->
                                if (transferSettings.readerAutomaticallySelectTransport) {
                                    connectionMethods[0]
                                } else {
                                    selectConnectionMethod(
                                        connectionMethods,
                                        connectionMethodPickerData
                                    )
                                }
                            },
                            requestDocument = requestDocument,
                            setDeviceResponseBytes = setDeviceResponseBytes
                        )
                    }
                )
            } catch (e: Throwable) {
                Napier.e("NFC engagement failed", e, tag)
            }
        }
    }

    private fun generateEncodedSessionTranscript(
        encodedDeviceEngagement: ByteArray,
        handover: DataItem,
        eReaderKey: EcPublicKey
    ): ByteArray {
        val encodedEReaderKey = Cbor.encode(eReaderKey.toCoseKey().toDataItem())
        //val s = EncodedSessionTranscript(encodedDeviceEngagement, encodedEReaderKey, handover.asArray.map { item -> item.asBstr })
        //val encoded = kotlinx.serialization.cbor.Cbor.encodeToByteArray(s)

        val cborEncoded = Cbor.encode(
            CborArray.builder()
                .add(Tagged(24, Bstr(encodedDeviceEngagement)))
                .add(Tagged(24, Bstr(encodedEReaderKey)))
                .add(handover)
                .end()
                .build()
        )
        return cborEncoded
    }

    suspend fun doQrFlow(
        qrCode: String,
        requestDocument: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit,
    ) {
        try {
            doReaderFlow(
                encodedDeviceEngagement = ByteString(qrCode.fromBase64Url()),
                existingTransport = null,
                handover = Simple.NULL,
                selectConnectionMethod = { connectionMethods ->
                    if (transferSettings.readerAutomaticallySelectTransport) {
                        connectionMethods[0]
                    } else {
                        selectConnectionMethod(
                            connectionMethods,
                            connectionMethodPickerData
                        )
                    }
                },
                requestDocument = requestDocument,
                setDeviceResponseBytes = setDeviceResponseBytes
            )
        } catch (error: Throwable) {
            Napier.e("Caught exception", error, tag)
            error.printStackTrace()
        }
    }

    private suspend fun doReaderFlow(
        encodedDeviceEngagement: ByteString,
        existingTransport: MdocTransport?,
        handover: DataItem,
        selectConnectionMethod: suspend (connectionMethods: List<ConnectionMethod>) -> ConnectionMethod?,
        requestDocument: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) {
        val deviceEngagement = EngagementParser(encodedDeviceEngagement.toByteArray()).parse()
        val eDeviceKey = deviceEngagement.eSenderKey
        val eReaderKey = Crypto.createEcPrivateKey(EcCurve.P256)

        val transport = if (existingTransport != null) {
            existingTransport
        } else {
            val connectionMethods = ConnectionMethod.disambiguate(deviceEngagement.connectionMethods)
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
                MdocTransport.Role.MDOC_READER,
                MdocTransportOptions(bleUseL2CAP = transferSettings.readerBleL2CapEnabled)
            )
            if (transport is NfcTransportMdocReader) {
                if (scanNfcTag(
                        message = "QR engagement with NFC Data Transfer. Move into NFC field of the mdoc",
                        tagInteractionFunc = { tag, updateMessage ->
                            Napier.d("doReaderFlow: tagInteractionFunc: $updateMessage", tag = this.tag)
                            transport.setTag(tag)
                            doReaderFlowWithTransport(
                                transport = transport,
                                encodedDeviceEngagement = encodedDeviceEngagement,
                                handover = handover,
                                eDeviceKey = eDeviceKey,
                                eReaderKey = eReaderKey,
                                requestDocument = requestDocument,
                                setDeviceResponseBytes = setDeviceResponseBytes
                            )
                            true
                        }
                    ) == true
                ) {
                    return
                } else {
                    throw IllegalStateException("Reading cancelled")
                }
            } else {
                transport
            }
        }
        doReaderFlowWithTransport(
            transport = transport,
            encodedDeviceEngagement = encodedDeviceEngagement,
            handover = handover,
            eDeviceKey = eDeviceKey,
            eReaderKey = eReaderKey,
            requestDocument = requestDocument,
            setDeviceResponseBytes = setDeviceResponseBytes
        )
    }

    private suspend fun doReaderFlowWithTransport(
        transport: MdocTransport,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        eDeviceKey: EcPublicKey,
        eReaderKey: EcPrivateKey,
        requestDocument: RequestDocument,
        setDeviceResponseBytes: (ByteArray) -> Unit
    ) {
        readerTransport.value = transport
        val encodedSessionTranscript = generateEncodedSessionTranscript(
            encodedDeviceEngagement.toByteArray(),
            handover,
            eReaderKey.publicKey
        )
        val sessionEncryption = SessionEncryption(
            SessionEncryption.Role.MDOC_READER,
            eReaderKey,
            eDeviceKey,
            encodedSessionTranscript,
        )
        readerSessionEncryption.value = sessionEncryption
        this.readerSessionTranscript = encodedSessionTranscript

        val encodedDeviceRequest = DeviceRequestGenerator(readerSessionTranscript!!).addDocumentRequest(
            docType = requestDocument.docType,
            itemsToRequest = requestDocument.itemsToRequest,
            requestInfo = null,
            readerKey = readerKey,
            signatureAlgorithm = readerKey.curve.defaultSigningAlgorithm,
            readerKeyCertificateChain = X509CertChain(listOf(readerCert, readerRootCert)),
        ).generate()

        try {
            transport.open(eDeviceKey)
            transport.sendMessage(
                sessionEncryption.encryptMessage(
                    messagePlaintext = encodedDeviceRequest,
                    statusCode = null
                )
            )
            while (true) {
                val sessionData = transport.waitForMessage()
                if (sessionData.isEmpty()) {
                    transport.close()
                    break
                }

                val (message, status) = sessionEncryption.decryptMessage(sessionData)
                Napier.i("Holder sent ${message?.size} bytes status $status", tag = tag)
                if (message != null) {
                    readerMostRecentDeviceResponse.value = message
                    setDeviceResponseBytes(message)
                    _state.value = State.DATA_RECEIVED
                }
                if (status == Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
                    Napier.i("Holder indicated they closed the connection. " +
                            "Closing and ending reader loop", tag = tag)
                    transport.close()
                    break
                }
                if (!transferSettings.presentmentAllowMultipleRequests) {
                    Napier.i("Holder did not indicate they are closing the connection. " +
                            "Auto-close is enabled, so sending termination message, closing, and " +
                            "ending reader loop", tag = tag)
                    transport.sendMessage(SessionEncryption.encodeStatus(Constants.SESSION_DATA_STATUS_SESSION_TERMINATION))
                    transport.close()
                    break
                }
                Napier.i("Holder did not indicate they are closing the connection. " +
                        "Auto-close is not enabled so waiting for message from holder", tag = tag)
                // "Send additional request" and close buttons will act further on `at.asitplus.wallet.verifier.transport`
            }
        } catch (_: MdocTransportClosedException) {
            // Nothing to do, this is thrown when at.asitplus.wallet.verifier.transport.close() is called from another coroutine, that
            // is, the onClick handlers for the close buttons.
            Napier.i("Ending reader flow due to MdocTransportClosedException", tag = tag)
        } finally {
            transport.close()
            readerTransport.value = null
        }
    }
}
