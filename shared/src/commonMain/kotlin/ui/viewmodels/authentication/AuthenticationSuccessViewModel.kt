package ui.viewmodels.authentication

class AuthenticationSuccessViewModel(
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val isCrossDeviceFlow: Boolean,
    val openRedirectUrl: (() -> Unit)?,
    val onClickSettings: () -> Unit
)
