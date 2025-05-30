package ui.viewmodels.iso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants.MDOC_PREFIX
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
import at.asitplus.wallet.lib.iso.DeviceResponse
import data.document.RequestDocument
import data.document.RequestDocumentBuilder
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// TODO: from acrusage: it seems like this manages navigation, maybe refactor this part into a navigation graph instead?
class VerifierViewModel(
    val walletMain: WalletMain,
    val settingsRepository: SettingsRepository,
): ViewModel() {
    private val transferManager: TransferManager by lazy {
        TransferManager(settingsRepository, viewModelScope) { message ->
            // TODO: handle update messages
        }
    }

    private val _verifierState = MutableStateFlow(VerifierState.INIT)
    val verifierState: StateFlow<VerifierState> = _verifierState

    fun setVerifierState(newVerifierState: VerifierState) {
        _verifierState.value = newVerifierState
    }

    private val _requestDocument = MutableStateFlow<RequestDocument?>(null)

    private val _deviceResponse = MutableStateFlow<DeviceResponse?>(null)
    val deviceResponse: StateFlow<DeviceResponse?> = _deviceResponse

    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage: StateFlow<String> = _errorMessage

    fun handleError(errorMessage: String) {
        _errorMessage.value = errorMessage
        setVerifierState(VerifierState.ERROR)
    }

    private val _selectedEngagementMethod = MutableStateFlow<DeviceEngagementMethods>(
        DeviceEngagementMethods.NFC
    )

    val selectedEngagementMethod: StateFlow<DeviceEngagementMethods> = _selectedEngagementMethod

    private fun setStateToEngagement(selectedEngagementMethod: DeviceEngagementMethods) {
        when (selectedEngagementMethod) {
            DeviceEngagementMethods.NFC -> doNfcEngagement()
            DeviceEngagementMethods.QR_CODE -> _verifierState.value = VerifierState.QR_ENGAGEMENT
        }
    }

    private fun doNfcEngagement() {
        _requestDocument.value?.let { document ->
            transferManager.startNfcEngagement(document) { deviceResponseBytes ->
                handleResponse(deviceResponseBytes)
            }
        }
    }

    private fun handleResponse(deviceResponseBytes: ByteArray) {
        _deviceResponse.value = DeviceResponse.deserialize(deviceResponseBytes).getOrThrow()
        _verifierState.value = VerifierState.PRESENTATION
    }

    fun onClickPredefinedMdlMandatoryAttributes(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getMdlMandatoryAttributesRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedMdlFullAttributes(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getMdlFullAttributesRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedAgeMdl(age: Int, selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getAgeVerificationRequestDocumentMdl(age)
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedPidRequiredAttributes(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getPidRequiredAttributesRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedPidFullAttributes(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getPidFullAttributesRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedAgePid(age: Int, selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getAgeVerificationRequestDocumentPid(age)
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedHidRequiredAttributes(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = RequestDocumentBuilder.getHealthIdRequiredAttributesRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun navigateToCustomSelectionView(selectedEngagementMethod: DeviceEngagementMethods) {
        _selectedEngagementMethod.value = selectedEngagementMethod
        _verifierState.value = VerifierState.SELECT_CUSTOM_REQUEST
    }

    fun navigateToVerifyDataView() {
        _verifierState.value = VerifierState.INIT
    }

    fun onReceiveCustomSelection(
        customSelectionDocument: RequestDocument,
        selectedEngagementMethod: DeviceEngagementMethods
    ) {
        _requestDocument.value = customSelectionDocument
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onFoundPayload(payload: String) {
        if (payload.startsWith(MDOC_PREFIX)) {
            _verifierState.value = VerifierState.WAITING_FOR_RESPONSE
            _requestDocument.value?.let { document ->
                transferManager.doQrFlow(
                    payload.removePrefix(MDOC_PREFIX),
                    document,
                    { message -> } // TODO: handle update messages
                ) { deviceResponseBytes ->
                    handleResponse(deviceResponseBytes)
                }
            }
        } else {
            val errorMessage = "Invalid QR-Code:\nQR-Code does not start with \"$MDOC_PREFIX\""
            Napier.e(errorMessage)
            handleError(errorMessage)
        }
    }
}

enum class VerifierState {
    INIT,
    SELECT_CUSTOM_REQUEST,
    QR_ENGAGEMENT,
    WAITING_FOR_RESPONSE,
    PRESENTATION,
    ERROR
}
