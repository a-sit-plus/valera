package ui.presentation

sealed interface UiState<out T> {
    fun <R> map(transform: (T) -> R): UiState<R>
}

data object UiStateLoading : UiState<Nothing> {
    override fun <R> map(transform: (Nothing) -> R): UiStateLoading = this
}

data class UiStateSuccess<out T>(
    val value: T
) : UiState<T> {
    override fun <R> map(transform: (T) -> R) = try {
        UiStateSuccess(
            transform(value)
        )
    } catch (it: Throwable) {
        UiStateError(it)
    }
}

data class UiStateError(
    val throwable: Throwable
) : UiState<Nothing> {
    override fun <R> map(transform: (Nothing) -> R): UiStateError = this
}