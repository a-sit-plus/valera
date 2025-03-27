package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ErrorService() {
    val error = MutableSharedFlow<Pair<String?, String?>>()
    fun emit (e: Throwable){
        CoroutineScope(Dispatchers.Main).launch {
            error.emit(Pair(e.message, e.cause?.message))
            Napier.e("Error", e)
        }
    }
}