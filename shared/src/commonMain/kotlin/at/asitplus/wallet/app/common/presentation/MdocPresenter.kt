package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.lib.iso.DeviceResponse
import com.android.identity.request.MdocClaim
import com.android.identity.cbor.Bstr
import com.android.identity.cbor.Cbor
import com.android.identity.cbor.CborArray
import com.android.identity.cbor.Tagged
import com.android.identity.document.NameSpacedData
import com.android.identity.documenttype.DocumentTypeRepository
import com.android.identity.mdoc.credential.MdocCredential
import com.android.identity.mdoc.mso.MobileSecurityObjectParser
import com.android.identity.mdoc.mso.StaticAuthDataParser
import com.android.identity.mdoc.request.DeviceRequestParser
import com.android.identity.mdoc.response.DocumentGenerator
import com.android.identity.mdoc.sessionencryption.SessionEncryption
import com.android.identity.mdoc.transport.MdocTransport
import com.android.identity.mdoc.transport.MdocTransportClosedException
import com.android.identity.mdoc.util.MdocUtil
import com.android.identity.mdoc.util.toMdocRequest
import com.android.identity.securearea.KeyUnlockInteractive
import com.android.identity.util.Constants
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import ui.viewmodels.PresentationStateModel
import ui.viewmodels.PresentationViewModel

class MdocPresenter(
    private val stateModel: PresentationStateModel,
    private val presentationViewModel: PresentationViewModel,
    private val mechanism: MdocPresentmentMechanism
) {
    private var sessionEncryption: SessionEncryption? = null

    internal suspend fun present(
        //source: PresentmentSource,
        dismissible: MutableStateFlow<Boolean>,
        numRequestsServed: MutableStateFlow<Int>,
        credentialSelected: (DeviceResponse) -> Unit,
        /*showCredentialPicker: suspend (
            documents: List<Credential>,
        ) -> Credential?,
        showConsentPrompt: suspend (
            document: Document,
            request: Request,
            trustPoint: TrustPoint?
        ) -> Boolean,*/
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
                    encodedSessionTranscript =
                        Cbor.encode(
                            CborArray.builder()
                                .add(
                                    Tagged(
                                        24,
                                        Bstr(mechanism.encodedDeviceEngagement.toByteArray())
                                    )
                                )
                                .add(
                                    Tagged(
                                        24,
                                        Bstr(Cbor.encode(eReaderKey.toCoseKey().toDataItem()))
                                    )
                                )
                                .add(mechanism.handover)
                                .end()
                                .build()
                        )
                    sessionEncryption = SessionEncryption(
                        SessionEncryption.Role.MDOC,
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

                //TODO use our libs to parse the device request
                val deviceRequest = DeviceRequestParser(
                    encodedDeviceRequest!!,
                    encodedSessionTranscript!!,
                ).parse()


                val mdocRequests = deviceRequest.docRequests.map {
                    it.toMdocRequest(
                        documentTypeRepository = DocumentTypeRepository(),
                        mdocCredential = null
                    )
                }

                presentationViewModel.initWithMdocRequest(mdocRequests, credentialSelected)

                val deviceResponse = stateModel.requestCredentialSelection()

                mechanism.transport.sendMessage(
                    sessionEncryption.encryptMessage(
                        deviceResponse.serialize(),
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
            error.printStackTrace()
            stateModel.setCompleted(error)
        }
    }


}

private suspend fun calcDocument(
    credential: MdocCredential,
    claims: List<MdocClaim>,
    encodedSessionTranscript: ByteArray
): ByteArray {
    val nsAndDataElements = mutableMapOf<String, MutableList<String>>()
    claims.forEach {
        nsAndDataElements.getOrPut(it.namespaceName, { mutableListOf() }).add(it.dataElementName)
    }

    val staticAuthData = StaticAuthDataParser(credential.issuerProvidedData).parse()

    val documentData = credential.document.metadata.nameSpacedData
    val mergedIssuerNamespaces = MdocUtil.mergeIssuerNamesSpaces(
        nsAndDataElements,
        documentData,
        staticAuthData
    )
    val issuerAuthCoseSign1 = Cbor.decode(staticAuthData.issuerAuth).asCoseSign1
    val encodedMsoBytes = Cbor.decode(issuerAuthCoseSign1.payload!!)
    val encodedMso = Cbor.encode(encodedMsoBytes.asTaggedEncodedCbor)
    val mso = MobileSecurityObjectParser(encodedMso).parse()

    val documentGenerator = DocumentGenerator(
        mso.docType,
        staticAuthData.issuerAuth,
        encodedSessionTranscript,
    )
    documentGenerator.setIssuerNamespaces(mergedIssuerNamespaces)

    val keyInfo = credential.secureArea.getKeyInfo(credential.alias)
    documentGenerator.setDeviceNamespacesSignature(
        NameSpacedData.Builder().build(),
        credential.secureArea,
        credential.alias,
        KeyUnlockInteractive(),
        keyInfo.publicKey.curve.defaultSigningAlgorithm,
    )
    return documentGenerator.generate()
}
