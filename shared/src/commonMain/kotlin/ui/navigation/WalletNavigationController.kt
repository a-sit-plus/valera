package ui.navigation

import ui.navigation.routes.Route

/**
 * Centralises all navigation actions so that platform-specific "finish" behaviour
 * (intentState.finishApp on iOS, finish() on Android) lives in one place rather than being
 * scattered across individual composables.
 */
interface WalletNavigationController {
    fun navigate(route: Route)
    fun navigateBack()
    fun popBackStack(route: Route)
    fun navigateNewGraph(route: Route)
    fun navigatePending()
    fun invocationAwareBack()
    fun popToInvoker()
    // Exposed on the interface because ErrorRoute composable needs it to decide how to wrap errors.
    fun shouldFinishToCaller(): Boolean
}