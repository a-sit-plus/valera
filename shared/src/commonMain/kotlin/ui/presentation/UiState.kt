package ui.presentation

sealed interface UiState<out T>

data object UiStateLoading : UiState<Nothing>

data class UiStateSuccess<out T>(
    val value: T
) : UiState<T>

data class UiStateError(
    val throwable: Throwable
) : UiState<Nothing>