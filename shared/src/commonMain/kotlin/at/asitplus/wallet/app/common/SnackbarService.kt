package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class SnackbarService {
    val message = MutableSharedFlow<SnackbarFlowData>()
    private val scope = CoroutineScope(Dispatchers.Default)
    /**
     * Shows a snackbar with [text] and action label [actionLabel], calls [callback] when user has executed action
     */
    fun showSnackbar(text: String, actionLabel: String? = null, callback: (() -> Unit)? = null) = scope.launch {
        Napier.d("Show Snackbar with text: $text")
        message.emit(SnackbarFlowData(text, actionLabel, callback))
    }
}

data class SnackbarFlowData (val text: String, val actionLabel: String?, val callback: (() -> Unit)?)
