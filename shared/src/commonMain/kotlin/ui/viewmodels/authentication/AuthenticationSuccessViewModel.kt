package ui.viewmodels.authentication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import ui.navigation.routes.AuthenticationSuccessRoute

class AuthenticationSuccessViewModel(
    savedStateHandle: SavedStateHandle,
    private val urlOpener: UrlOpener,
): ViewModel() {
    private val route = savedStateHandle.toRoute<AuthenticationSuccessRoute>()
    val isCrossDeviceFlow = route.isCrossDeviceFlow
    val redirectUrl = route.redirectUrl

    fun openRedirectUrl(redirectUrl: String) {
        urlOpener(redirectUrl)
    }
}