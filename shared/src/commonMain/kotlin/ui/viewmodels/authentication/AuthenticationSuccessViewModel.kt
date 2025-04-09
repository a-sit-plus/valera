package ui.viewmodels.authentication

class AuthenticationSuccessViewModel(
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val openRedirectUrl: (() -> Unit)?,
)