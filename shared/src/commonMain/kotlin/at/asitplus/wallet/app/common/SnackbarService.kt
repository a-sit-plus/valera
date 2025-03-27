package at.asitplus.wallet.app.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SnackbarService {
    val message = MutableSharedFlow<Pair<(() -> Unit)?,Pair<String, String?>>>()
    private val scope = CoroutineScope(Dispatchers.Default)
    /**
     * Shows a snackbar with [text] and action label [actionLabel], calls [callback] when user has executed action
     */
    fun showSnackbar(text: String, actionLabel: String? = null, callback: (() -> Unit)? = null) {
        Napier.d("Show Snackbar with text: $text")
        scope.launch {
            message.emit(Pair(callback, Pair(text, actionLabel)))
        }
    }
}