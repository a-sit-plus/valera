package ui.viewmodels

class ErrorViewModel(
    val resetStack: () -> Unit,
    val message: String?,
    val cause: String?,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit
)