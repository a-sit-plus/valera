package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ErrorService() {
    val error = MutableSharedFlow<ErrorFlowData>()
    private val scope = CoroutineScope(Dispatchers.Default)
    
    fun emit (e: Throwable){
        scope.launch {
            error.emit(ErrorFlowData(e.message, e.cause?.message))
            Napier.e("Error", e)
        }
    }
}

data class ErrorFlowData(val message: String?, val cause: String?)