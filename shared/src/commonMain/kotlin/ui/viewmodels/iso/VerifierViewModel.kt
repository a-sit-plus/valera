package ui.viewmodels.iso

import at.asitplus.KmmResult
import at.asitplus.iso.DeviceResponse
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants.MDOC_PREFIX
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
import data.document.RequestDocumentBuilder
import data.document.RequestDocumentList
import data.document.SelectableRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromByteArray

class VerifierViewModel(
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val walletMain: WalletMain,
    val navigateToHomeScreen: () -> Unit,
    val onClickSettings: () -> Unit,
    val settingsRepository: SettingsRepository,
) {
    private val transferManager: TransferManager by lazy {
        TransferManager(settingsRepository, walletMain.scope) { message -> } // TODO: handle update messages
    }

    private val _verifierState = MutableStateFlow(VerifierState.INIT)
    val verifierState: StateFlow<VerifierState> = _verifierState

    private val _requestDocumentList = RequestDocumentList()

    private val _deviceResponse = MutableStateFlow<DeviceResponse?>(null)
    val deviceResponse: StateFlow<DeviceResponse?> = _deviceResponse

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    fun handleError(errorMessage: String) {
        _errorMessage.value = errorMessage
        _verifierState.value = VerifierState.ERROR
    }

    private val _selectedEngagementMethod = MutableStateFlow(DeviceEngagementMethods.QR_CODE)
    val selectedEngagementMethod: StateFlow<DeviceEngagementMethods> = _selectedEngagementMethod

    private fun setStateToEngagement(selectedEngagementMethod: DeviceEngagementMethods) {
        when (selectedEngagementMethod) {
            DeviceEngagementMethods.NFC -> doNfcEngagement()
            DeviceEngagementMethods.QR_CODE -> _verifierState.value = VerifierState.QR_ENGAGEMENT
        }
    }

    private fun doNfcEngagement() {
        _requestDocumentList.let { requestDocumentList ->
            transferManager.startNfcEngagement(requestDocumentList) { deviceResponseBytes ->
                handleResponse(deviceResponseBytes)
            }
        }
    }

    private fun handleResponse(result: KmmResult<ByteArray>) {
        result.onSuccess { deviceResponseBytes ->
            _deviceResponse.value = coseCompliantSerializer.decodeFromByteArray<DeviceResponse>(deviceResponseBytes)
            _verifierState.value = VerifierState.PRESENTATION
        }.onFailure { error ->
            handleError(error.message ?: "Unknown error")
        }
    }

    fun onRequestSelected(
        selectedEngagementMethod: DeviceEngagementMethods,
        request: SelectableRequest
    ) {
        _requestDocumentList.addRequestDocument(
            RequestDocumentBuilder.buildRequestDocument(request)
        )
        setStateToEngagement(selectedEngagementMethod)
    }

    fun navigateToCustomSelectionView(selectedEngagementMethod: DeviceEngagementMethods) {
        _selectedEngagementMethod.value = selectedEngagementMethod
        _verifierState.value = VerifierState.SELECT_CUSTOM_REQUEST
    }

    fun navigateToCombinedSelectionView(selectedEngagementMethod: DeviceEngagementMethods) {
        _selectedEngagementMethod.value = selectedEngagementMethod
        _verifierState.value = VerifierState.SELECT_COMBINED_REQUEST
    }

    fun navigateToVerifyDataView() {
        _verifierState.value = VerifierState.INIT
    }

    fun onReceiveCombinedSelection(requestSelectionList:  List<SelectableRequest>) {
        requestSelectionList.forEach { request ->
            _requestDocumentList.addRequestDocument(
                RequestDocumentBuilder.buildRequestDocument(request)
            )
        }
        setStateToEngagement(selectedEngagementMethod.value)
    }

    fun onReceiveCustomSelection(
        selectedDocumentType: String,
        selectedEntries: Collection<String>
    ) {
        val config = RequestDocumentBuilder.getDocTypeConfig(selectedDocumentType) ?: return
        _requestDocumentList.addRequestDocument(
            RequestDocumentBuilder.buildRequestDocument(
                scheme = config.scheme,
                subSet = selectedEntries
            )
        )
        setStateToEngagement(selectedEngagementMethod.value)
    }

    val onFoundPayload: (String) -> Unit = { payload ->
        if (payload.startsWith(MDOC_PREFIX)) {
            _verifierState.value = VerifierState.WAITING_FOR_RESPONSE
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
            val errorMessage = "Invalid QR-Code:\nQR-Code does not start with \"$MDOC_PREFIX\""
            Napier.e(errorMessage)
            handleError(errorMessage)
        }
    }
}

enum class VerifierState {
    INIT,
    SELECT_CUSTOM_REQUEST,
    SELECT_COMBINED_REQUEST,
    QR_ENGAGEMENT,
    WAITING_FOR_RESPONSE,
    PRESENTATION,
    ERROR
}
