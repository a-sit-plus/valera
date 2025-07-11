package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ErrorService() {
    val error = MutableSharedFlow<ErrorFlowData>(replay = 1)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val map = mutableMapOf<String, Throwable>()

    fun get(throwableId: String): Throwable? = map.remove(throwableId)

    @OptIn(ExperimentalUuidApi::class, ExperimentalUuidApi::class)
    fun put(throwable: Throwable): String {
        val id = Uuid.random().toString()
        map[id] = throwable
        return id
    }

    fun emit(e: Throwable) = scope.launch {
        val id = put(e)
        error.emit(ErrorFlowData(id))
        Napier.e("Error", e)
    }
}

data class ErrorFlowData(val throwableId: String)