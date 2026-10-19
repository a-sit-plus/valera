package ui.viewmodels.iso.verifier

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.iso.DeviceResponse
import at.asitplus.iso.Document
import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.presentation_canceled
import at.asitplus.valera.resources.snackbar_nfc_tag_lost_retrying
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants.MDOC_PREFIX
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.state.TransferTransport
import at.asitplus.wallet.app.common.iso.transfer.state.VerifierState
import at.asitplus.wallet.app.common.iso.verifier.DeviceResponseException
import at.asitplus.wallet.app.common.iso.verifier.ProximityResponseValidator
import at.asitplus.wallet.app.common.iso.verifier.VerifyResponseException
import at.asitplus.wallet.app.common.presentation.NfcDispatchSuppressionMode
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import at.asitplus.wallet.app.common.presentation.PresentmentCanceled
import at.asitplus.wallet.app.common.validation.ValidationOutcomeUi
import at.asitplus.wallet.app.common.validation.toUi
import at.asitplus.wallet.lib.data.CredentialScheme
import data.document.RequestDocumentBuilder
import data.document.RequestDocumentList
import data.document.SelectableRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import org.jetbrains.compose.resources.getString
import ui.viewmodels.iso.common.TransferOptionsViewModel

class VerifierViewModel(
    walletMain: WalletMain,
    settingsRepository: SettingsRepository,
    private val proximityResponseValidator: ProximityResponseValidator,
) : TransferOptionsViewModel(walletMain, settingsRepository) {
    private val transferManager: TransferManager by lazy {
        TransferManager(
            settingsRepository,
            walletMain.scope,
            updateProgress = { _ -> },
            onWarning = { warning ->
                when (warning) {
                    TransferManager.Warning.NFC_TAG_LOST_RETRYING -> {
                        walletMain.scope.launch {
                            walletMain.snackbarService.showSnackbar(
                                getString(Res.string.snackbar_nfc_tag_lost_retrying)
                            )
                        }
                    }
                }
            },
            onTransportSelected = { transport ->
                Napier.i("Verifier transport selected; transport=$transport", tag = "VerifierViewModel")
                if (transport == TransferTransport.NFC) NfcTransferState.verifierNfcTransferActive.value = true
                setState(VerifierState.NfcTransferring(transport))
            },
            onNfcDataTransferSelected = {
                Napier.i("Verifier QR flow selected NFC data transfer; waiting for device tap", tag = "VerifierViewModel")
                setState(VerifierState.QrNfcDataTransferTap)
            }
        )
    }

    private val _verifierState = MutableStateFlow<VerifierState>(VerifierState.Settings)
    val verifierState: StateFlow<VerifierState> = _verifierState

    fun setState(newState: VerifierState) {
        if (_verifierState.value == newState) return
        Napier.d("Change state from ${_verifierState.value} to $newState", tag = "VerifierViewModel")
        _verifierState.value = newState
    }

    private val _requestDocumentList = RequestDocumentList()

    private val _responseDocumentList = mutableListOf<ReceivedDocumentUi>()
    val responseDocumentList: List<ReceivedDocumentUi> = _responseDocumentList

    val onResume: () -> Unit = {
        NfcTransferState.holderNfcDataTransferActive.value = false
        NfcTransferState.verifierNfcReaderModeActive.value = false
        NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.NONE
        NfcTransferState.verifierNfcTransferActive.value = false
        setState(VerifierState.Settings)
        _requestDocumentList.clear()
        engagementPreviousState = VerifierState.SelectDocument
    }

    // Tracks which selection state the user came from before QrEngagement, so back can return there.
    var engagementPreviousState: VerifierState = VerifierState.SelectDocument
        private set

    val onConsentSettings: () -> Unit = { setState(VerifierState.CheckSettings) }

    private val _throwable = MutableStateFlow<Throwable?>(null)
    val throwable: StateFlow<Throwable?> = _throwable

    private fun handleError(throwable: Throwable) {
        _throwable.value = throwable
        setState(VerifierState.Error)
    }

    private fun setStateToEngagement(selectedEngagementMethod: DeviceEngagementMethods) {
        when (selectedEngagementMethod) {
            DeviceEngagementMethods.NFC -> doNfcEngagement()
            DeviceEngagementMethods.QR_CODE -> setState(VerifierState.QrEngagement)
        }
    }

    private fun doNfcEngagement() {
        NfcTransferState.nfcDataTransferActive.value = false
        NfcTransferState.holderNfcDataTransferActive.value = false
        NfcTransferState.verifierNfcReaderModeActive.value = false
        NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.NONE
        setState(VerifierState.NfcEngagement)
        _requestDocumentList.let { requestDocumentList ->
            transferManager.startNfcEngagement(requestDocumentList) { deviceResponseResult ->
                handleResponse(deviceResponseResult)
            }
        }
    }

    fun cancelNfcEngagement() {
        Napier.i("Cancelling verifier NFC engagement/data transfer", tag = "VerifierViewModel")
        NfcTransferState.verifierNfcReaderModeActive.value = false
        NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.NONE
        NfcTransferState.verifierNfcTransferActive.value = false
        transferManager.cancel()
        onResume()
    }

    private fun handleResponse(result: KmmResult<ByteArray>) {
        Napier.i("Verifier response callback received; clearing NFC transfer active flag", tag = "VerifierViewModel")
        NfcTransferState.verifierNfcReaderModeActive.value = false
        NfcTransferState.verifierNfcTransferActive.value = false
        result.onSuccess { deviceResponseBytes ->
            Napier.d("deviceResponseBytes =\n${deviceResponseBytes.toHexString()}")
            try {
                val deviceResponse = coseCompliantSerializer.decodeFromByteArray<DeviceResponse>(deviceResponseBytes)
                checkResponse(deviceResponse)
            } catch (e: Exception) {
                handleError(DeviceResponseException("Failed to decode DeviceResponse", e, deviceResponseBytes))
            }
        }.onFailure {
            if (it is PresentmentCanceled) {
                handleHolderCanceled()
            } else {
                handleError(it)
            }
        }
    }

    private fun handleHolderCanceled() {
        Napier.i("Holder canceled verifier presentment; returning to previous screen", tag = "VerifierViewModel")
        walletMain.scope.launch {
            walletMain.snackbarService.showSnackbar(getString(Res.string.presentation_canceled))
            delay(700)
            setState(engagementPreviousState)
        }
    }

    private fun checkResponse(deviceResponse: DeviceResponse) {
        setState(VerifierState.CheckResponse)
        val sessionTranscript = transferManager.readerSessionTranscript
        walletMain.scope.launch(Dispatchers.IO) {
            catchingUnwrapped {
                proximityResponseValidator.validate(deviceResponse, sessionTranscript)
            }.onSuccess { validations ->
                _responseDocumentList.clear()
                _responseDocumentList.addAll(validations.map { ReceivedDocumentUi(it.document, it.report.toUi()) })
                setState(VerifierState.Presentation)
            }.onFailure {
                handleError(it as? VerifyResponseException ?: VerifyResponseException("Unable to validate response", it))
            }
        }
    }


    fun onRequestSelected(request: SelectableRequest) {
        engagementPreviousState = VerifierState.SelectDocument
        _requestDocumentList.addRequestDocument(
            RequestDocumentBuilder.buildRequestDocument(request)
        )
        setStateToEngagement(selectedEngagementMethod.value)
    }

    fun navigateToCustomSelectionView() {
        setState(VerifierState.SelectCustomRequest)
    }

    fun navigateToCombinedSelectionView() {
        setState(VerifierState.SelectCombinedRequest)
    }

    fun onReceiveCombinedSelection(requestSelectionList: List<SelectableRequest>) {
        engagementPreviousState = VerifierState.SelectCombinedRequest
        requestSelectionList.forEach { request ->
            _requestDocumentList.addRequestDocument(
                RequestDocumentBuilder.buildRequestDocument(request)
            )
        }
        setStateToEngagement(selectedEngagementMethod.value)
    }

    fun onReceiveCustomSelection(
        selectedScheme: CredentialScheme,
        selectedEntries: Collection<OpenId4VciClaimsPathPointer>
    ) {
        engagementPreviousState = VerifierState.SelectCustomRequest
        _requestDocumentList.addRequestDocument(
            RequestDocumentBuilder.buildRequestDocument(selectedScheme, selectedEntries)
        )
        setStateToEngagement(selectedEngagementMethod.value)
    }

    val onFoundPayload: (String) -> Unit = { payload ->
        if (payload.startsWith(MDOC_PREFIX)) {
            _requestDocumentList.let { requestDocumentList ->
                transferManager.doQrFlow(
                    payload.removePrefix(MDOC_PREFIX),
                    requestDocumentList,
                    { message -> Napier.d("Transfer message: $message") } // TODO: handle update messages
                ) { deviceResponseBytes ->
                    handleResponse(deviceResponseBytes)
                }
            }
        } else {
            handleError(IllegalArgumentException("Invalid QR-Code:\nQR-Code does not start with \"$MDOC_PREFIX\""))
        }
    }
}

/**
 * A [document] the verifier received, with the [validation] of it, see [ProximityResponseValidator]. Its claims are only
 * shown if its integrity has been verified, see [ValidationOutcomeUi.integrityFailed].
 */
data class ReceivedDocumentUi(
    val document: Document,
    val validation: ValidationOutcomeUi,
)
