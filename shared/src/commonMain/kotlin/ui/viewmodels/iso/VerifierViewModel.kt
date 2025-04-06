package ui.viewmodels.iso

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
import data.document.RequestDocument
import data.document.getAgeVerificationRequestDocument
import data.document.getIdentityRequestDocument
import data.document.getMdlRequestDocument
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VerifierViewModel(
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val walletMain: WalletMain,
//    val onSuccess: (RequestDocument, String) -> Unit
) {

    private val transferManager: TransferManager by lazy {
        TransferManager(
            walletMain.scope
        ) { message -> } //TODO handle update messages
    }

    private val _verifierState = MutableStateFlow(VerifierState.INIT)
    val verifierState: StateFlow<VerifierState> = _verifierState

    private val _requestDocument = MutableStateFlow<RequestDocument?>(null)
    private val requestDocument: StateFlow<RequestDocument?> = _requestDocument

    private fun setStateToEngagement(selectedEngagementMethod: DeviceEngagementMethods) {
        when (selectedEngagementMethod) {
            DeviceEngagementMethods.NFC -> doNfcEngagement()
            DeviceEngagementMethods.QR_CODE -> _verifierState.value = VerifierState.QR_ENGAGEMENT
        }
    }

    private fun doNfcEngagement() {
        requestDocument.value?.let { document ->
            transferManager.startNfcEngagement(document) { deviceResponseBytes ->
                handleResponse(deviceResponseBytes)
            }
        }
    }

    private fun handleResponse(deviceResponseBytes: ByteArray) {
        Napier.d(
            "deviceResponseBytes = ${deviceResponseBytes.decodeToString()}",
            tag = "VerifierViewModel"
        )
        //TODO show exceptions in error view
        TODO("Handle response")
    }

    fun onClickPredefinedIdentity(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = getIdentityRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedMdl(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = getMdlRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedAge(age: Int, selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = getAgeVerificationRequestDocument(age)
        setStateToEngagement(selectedEngagementMethod)
    }

    fun navigateToCustomSelectionView() {
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

    val onFoundPayload: (String) -> Unit = { payload ->
        if (payload.startsWith("mdoc:")) {
            _verifierState.value = VerifierState.WAITING_FOR_RESPONSE
            requestDocument.value?.let { document ->
                transferManager.doQrFlow(
                    payload.substring(5),
                    document,
                    { message -> } //TODO handle update messages
                ) { deviceResponseBytes ->
                    handleResponse(deviceResponseBytes)
                }
            }
        } else {
            Napier.w("QR-Code does not start with \"mdoc:\"")
            TODO("handle onFailure()")
        }
    }
}

enum class VerifierState {
    INIT,
    SELECT_CUSTOM_REQUEST,
    QR_ENGAGEMENT,
    WAITING_FOR_RESPONSE,
    PRESENTATION
}
