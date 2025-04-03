package ui.viewmodels.iso

import at.asitplus.wallet.app.common.transfer.TransferManager
import data.document.RequestDocument
import data.document.getAgeVerificationRequestDocument
import data.document.getIdentityRequestDocument
import data.document.getMdlRequestDocument
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VerifierViewModel(
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
//    val onSuccess: (RequestDocument, String) -> Unit
) {
    private val transferManager: TransferManager by lazy {
        TransferManager(
            CoroutineScope(
                Dispatchers.IO
            )
        )
    }

    private val _verifierState = MutableStateFlow(VerifierState.INIT)
    val verifierState: StateFlow<VerifierState> = _verifierState

    private val _requestDocument = MutableStateFlow<RequestDocument?>(null)
    private val requestDocument: StateFlow<RequestDocument?> = _requestDocument

    fun onClickPredefinedIdentity() {
        _requestDocument.value = getIdentityRequestDocument()
        _verifierState.value = VerifierState.QR_ENGAGEMENT
    }

    fun onClickPredefinedMdl() {
        _requestDocument.value = getMdlRequestDocument()
        _verifierState.value = VerifierState.QR_ENGAGEMENT
    }

    fun onClickPredefinedAge(age: Int) {
        _requestDocument.value = getAgeVerificationRequestDocument(age)
        _verifierState.value = VerifierState.QR_ENGAGEMENT
    }

    fun navigateToCustomSelectionView() {
        _verifierState.value = VerifierState.SELECT_CUSTOM_REQUEST
    }

    fun navigateToVerifyDataView() {
        _verifierState.value = VerifierState.INIT
    }

    fun onReceiveCustomSelection(customSelectionDocument: RequestDocument) {
        _requestDocument.value = customSelectionDocument
        _verifierState.value = VerifierState.QR_ENGAGEMENT
    }

    val onFoundPayload: (String) -> Unit = { payload ->
        if (payload.startsWith("mdoc:")) {
            _verifierState.value = VerifierState.WAITING_FOR_RESPONSE
            requestDocument.value?.let { document ->
                transferManager.doQrFlow(payload.substring(5), document, { deviceResponseBytes ->
                    Napier.d("deviceResponseBytes = $deviceResponseBytes", tag = "VerifierViewModel")
                    // TODO: handle onSuccess()
                })
            }
        } else {
            Napier.w("QR-Code does not start with \"mdoc:\"")
            // TODO: handle onFailure()
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
