package ui.viewmodels

sealed interface UiState<T> {
    class Loading<T>() : UiState<T> {
        override fun <R> map(transform: (T) -> R) = UiState.Loading<R>()
    }

    class Success<T>(val value: T) : UiState<T> {
        override fun <R> map(transform: (T) -> R) = UiState.Success<R>(
            transform(value)
        )
    }

    class Failure<T>(val throwable: Throwable) : UiState<T> {
        override fun <R> map(transform: (T) -> R) = UiState.Loading<R>()
    }

    fun <R> map(transform: (T) -> R): UiState<R>
}