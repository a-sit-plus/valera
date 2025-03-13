package at.asitplus.wallet.app.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SnackbarService(private val scope: CoroutineScope) {
    val call = MutableSharedFlow<Pair<() -> Unit,Pair<String, String>>>()
    val message = MutableSharedFlow<Pair<String, String?>>()

    fun showSnackbar(text: String) {
        Napier.d("Show Snackbar with text: $text")
        scope.launch {
            message.emit(Pair(text, null))
        }
    }

    /**
     * Shows a snackbar with [text] and action label [actionLabel], calls [callback] when user has executed action
     */
    fun showSnackbar(text: String, actionLabel: String, callback: () -> Unit) {
        Napier.d("Show Snackbar with text: $text")
        scope.launch {
            call.emit(Pair(callback, Pair(text, actionLabel)))
        }
    }
}