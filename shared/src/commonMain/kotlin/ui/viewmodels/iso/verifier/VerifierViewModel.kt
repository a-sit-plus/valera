package ui.viewmodels.iso.verifier

import at.asitplus.KmmResult
import at.asitplus.iso.DeviceResponse
import at.asitplus.iso.Document
import at.asitplus.iso.MobileSecurityObject
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants.MDOC_PREFIX
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.state.VerifierState
import at.asitplus.wallet.app.common.iso.verifier.DeviceResponseException
import at.asitplus.wallet.app.common.iso.verifier.VerifyResponseException
import at.asitplus.wallet.lib.agent.ValidatorVcJws
import at.asitplus.wallet.lib.agent.Verifier.VerifyPresentationResult
import at.asitplus.wallet.lib.agent.VerifierAgent
import at.asitplus.wallet.lib.data.IsoDocumentParsed
import data.document.RequestDocumentBuilder
import data.document.RequestDocumentList
import data.document.SelectableRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import ui.viewmodels.iso.common.TransferViewModel

class VerifierViewModel(
    walletMain: WalletMain,
    settingsRepository: SettingsRepository
) : TransferViewModel(walletMain, settingsRepository) {

    val onResume: () -> Unit = { setState(VerifierState.Settings) }
    val onConsentSettings: () -> Unit = { setState(VerifierState.CheckSettings) }

    private val transferManager: TransferManager by lazy {
        TransferManager(settingsRepository, walletMain.scope) { message -> } // TODO: handle update messages
    }

    private val _verifierState = MutableStateFlow<VerifierState>(VerifierState.Settings)
    val verifierState: StateFlow<VerifierState> = _verifierState

    fun setState(newState: VerifierState) {
        if (_verifierState.value == newState) return
        Napier.d("Change state from ${_verifierState.value} to $newState", tag = "VerifierViewModel")
        _verifierState.value = newState
    }

    private val _requestDocumentList = RequestDocumentList()

    private val _responseDocumentList = mutableListOf<IsoDocumentParsed>()
    val responseDocumentList: MutableList<IsoDocumentParsed> = _responseDocumentList

    private val _throwable = MutableStateFlow<Throwable?>(null)
    val throwable: StateFlow<Throwable?> = _throwable

    private fun handleError(throwable: Throwable) {
        _throwable.value = throwable
        setState(VerifierState.Error)
    }

    private val _selectedEngagementMethod = MutableStateFlow(DeviceEngagementMethods.QR_CODE)
    val selectedEngagementMethod: StateFlow<DeviceEngagementMethods> = _selectedEngagementMethod

    fun setEngagementMethod(method: DeviceEngagementMethods) {
        if(_selectedEngagementMethod.value == method) return
        _selectedEngagementMethod.value = method
    }

    private fun setStateToEngagement(selectedEngagementMethod: DeviceEngagementMethods) {
        when (selectedEngagementMethod) {
            DeviceEngagementMethods.NFC -> doNfcEngagement()
            DeviceEngagementMethods.QR_CODE -> setState(VerifierState.QrEngagement)
        }
    }

    private fun doNfcEngagement() {
        _requestDocumentList.let { requestDocumentList ->
            transferManager.startNfcEngagement(requestDocumentList) { deviceResponseResult ->
                handleResponse(deviceResponseResult)
            }
        }
    }

    private fun handleResponse(result: KmmResult<ByteArray>) {
        result.onSuccess { deviceResponseBytes ->
            Napier.d("deviceResponseBytes =\n${deviceResponseBytes.toHexString()}")
            try {
                val deviceResponse = coseCompliantSerializer.decodeFromByteArray<DeviceResponse>(deviceResponseBytes)
                checkResponse(deviceResponse)
            } catch (e: Exception) {
                handleError(DeviceResponseException("Failed to decode DeviceResponse", e, deviceResponseBytes))
            }
        }.onFailure { handleError(it) }
    }

    private fun checkResponse(deviceResponse: DeviceResponse) {
        setState(VerifierState.CheckResponse)
        val verifyDocument: suspend (MobileSecurityObject, Document) -> Boolean = { _, doc ->
            // TODO: verification of device authentication
            true
        }
        walletMain.scope.launch(Dispatchers.IO) {
            try {
                val verifierAgent = VerifierAgent("Proximity Verifier")
                when (val result = verifierAgent.verifyPresentationIsoMdoc(deviceResponse, verifyDocument)) {
                    is VerifyPresentationResult.SuccessIso -> {
                        responseDocumentList.addAll(result.documents)
                        setState(VerifierState.Presentation)
                    }
                    is VerifyPresentationResult.ValidationError -> {
                        handleError(VerifyResponseException("Verification failed: ValidationError", result.cause))
                        return@launch
                    }
                    else -> {
                        handleError(VerifyResponseException("Unsupported verification result"))
                        return@launch
                    }
                }
            } catch (e: Exception) {
                handleError(VerifyResponseException("Verification of response failed", e))
            }
        }
    }

    fun onRequestSelected(request: SelectableRequest) {
        _requestDocumentList.addRequestDocument(
            RequestDocumentBuilder.buildRequestDocument(request)
        )
        setStateToEngagement(_selectedEngagementMethod.value)
    }

    fun navigateToCustomSelectionView() {
        setState(VerifierState.SelectCustomRequest)
    }

    fun navigateToCombinedSelectionView() {
        setState(VerifierState.SelectCombinedRequest)
    }

    fun onReceiveCombinedSelection(requestSelectionList: List<SelectableRequest>) {
        requestSelectionList.forEach { request ->
            _requestDocumentList.addRequestDocument(
                RequestDocumentBuilder.buildRequestDocument(request)
            )
        }
        setStateToEngagement(_selectedEngagementMethod.value)
    }

    fun onReceiveCustomSelection(
        selectedDocumentType: String,
        selectedEntries: Collection<String>
    ) {
        val config = RequestDocumentBuilder.getDocTypeConfig(selectedDocumentType) ?: return
        _requestDocumentList.addRequestDocument(
            RequestDocumentBuilder.buildRequestDocument(config.scheme, selectedEntries)
        )
        setStateToEngagement(_selectedEngagementMethod.value)
    }

    val onFoundPayload: (String) -> Unit = { payload ->
        if (payload.startsWith(MDOC_PREFIX)) {
            setState(VerifierState.WaitingForResponse)
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
