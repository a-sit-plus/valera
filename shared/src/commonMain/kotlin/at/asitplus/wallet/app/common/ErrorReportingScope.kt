package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createErrorReportingScope(
    name: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    errorServiceProvider: () -> ErrorService?,
): CoroutineScope {
    val exceptionHandler = CoroutineExceptionHandler { _, error ->
        val errorService = runCatching(errorServiceProvider).getOrNull()
        if (errorService != null) {
            runCatching {
                errorService.emit(error)
            }.onFailure {
                Napier.e("Failed to report uncaught coroutine exception from $name", it)
                Napier.e("Uncaught coroutine exception from $name", error)
            }
        } else {
            Napier.e("Uncaught coroutine exception from $name", error)
        }
    }

    return CoroutineScope(
        SupervisorJob() + dispatcher + CoroutineName(name) + exceptionHandler
    )
}
