package ui.viewmodels.iso

import at.asitplus.KmmResult
import at.asitplus.iso.DeviceResponse
import at.asitplus.iso.Document
import at.asitplus.iso.MobileSecurityObject
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants.MDOC_PREFIX
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
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

    private val _responseDocumentList = mutableListOf<IsoDocumentParsed>()
    val responseDocumentList: MutableList<IsoDocumentParsed> = _responseDocumentList

    private val _throwable = MutableStateFlow<Throwable?>(null)
    val throwable: StateFlow<Throwable?> = _throwable

    private fun handleError(throwable: Throwable) {
        _throwable.value = throwable
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
        _verifierState.value = VerifierState.CHECK_RESPONSE
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
                        _verifierState.value = VerifierState.PRESENTATION
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

    fun onReceiveCombinedSelection(requestSelectionList: List<SelectableRequest>) {
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
            RequestDocumentBuilder.buildRequestDocument(config.scheme, selectedEntries)
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
            handleError(IllegalArgumentException("Invalid QR-Code:\nQR-Code does not start with \"$MDOC_PREFIX\""))
        }
    }
}

enum class VerifierState {
    INIT,
    SELECT_CUSTOM_REQUEST,
    SELECT_COMBINED_REQUEST,
    QR_ENGAGEMENT,
    WAITING_FOR_RESPONSE,
    CHECK_RESPONSE,
    PRESENTATION,
    ERROR
}
