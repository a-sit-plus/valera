package at.asitplus.wallet.app.common

import androidx.compose.material3.SnackbarHostState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarService(val scope: CoroutineScope, val snackbarHostState: SnackbarHostState) {
    fun showSnackbar(text: String){
        Napier.d("Show Snackbar with text: $text")
        scope.launch {
            snackbarHostState.showSnackbar(text)
        }
    }
}