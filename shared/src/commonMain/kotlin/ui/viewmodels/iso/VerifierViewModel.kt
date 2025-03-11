package ui.viewmodels.iso

import data.document.getAgeVerificationRequestDocument
import data.document.getIdentityRequestDocument
import data.document.getMdlRequestDocument
import data.document.RequestDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VerifierViewModel (
    val navigateUp: () -> Unit,
//    val onSuccess: (RequestDocument, String) -> Unit,
) {
    val onFoundPayload: (String) -> Unit = { payload ->
        requestDocument.value?.let { document ->
//            onSuccess(document, payload)


        }

        // TODO: set up transfer manager and startQrEngagement
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
}

enum class VerifierState {
    INIT,
    SELECT_CUSTOM_REQUEST,
    QR_ENGAGEMENT,
    PRESENTATION
}
