package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData
import kotlinx.coroutines.flow.MutableStateFlow
import ui.viewmodels.authentication.PresentationStateModel

class IntentState {
    val appLink = MutableStateFlow<String?>(null)
    val dcapiInvocationData = MutableStateFlow<DCAPIInvocationData?>(null)
    val presentationStateModel = MutableStateFlow<PresentationStateModel?>(null)
    var finishApp: (() -> Unit)? = null
}
