package at.asitplus.wallet.app.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarService(private val scope: CoroutineScope, private val snackbarHostState: SnackbarHostState) {
    fun showSnackbar(text: String) {
        Napier.d("Show Snackbar with text: $text")
        scope.launch {
            snackbarHostState.showSnackbar(message = text, withDismissAction = true)
        }
    }

    /**
     * Shows a snackbar with [text] and action label [actionLabel], calls [callback] when user has executed action
     */
    fun showSnackbar(text: String, actionLabel: String, callback: () -> Unit) {
        Napier.d("Show Snackbar with text: $text")
        scope.launch {
            val result = snackbarHostState.showSnackbar(message = text, actionLabel = actionLabel, withDismissAction = true)
            when (result) {
                SnackbarResult.Dismissed -> {}
                SnackbarResult.ActionPerformed -> callback.invoke()
            }
        }
    }
}