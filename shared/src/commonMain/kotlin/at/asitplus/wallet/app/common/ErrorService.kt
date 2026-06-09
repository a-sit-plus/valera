package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ErrorService(
    private val scope: CoroutineScope
) {
    val error = MutableSharedFlow<ErrorFlowData>(replay = 1)

    fun emit(e: Throwable) = scope.launch {
        error.emit(ErrorFlowData(e))
        Napier.e("Error", e)
    }

    fun clear() {
        error.resetReplayCache()
    }
}

data class ErrorFlowData(val throwable: Throwable)
