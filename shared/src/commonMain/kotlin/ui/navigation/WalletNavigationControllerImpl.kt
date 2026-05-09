package ui.navigation

import androidx.navigation.NavHostController
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.IntentState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.navigation.routes.CapabilitiesRoute
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.PrerequisiteRoute
import ui.navigation.routes.Route

internal open class WalletNavigationControllerImpl(
    protected val navController: NavHostController,
    protected val scope: CoroutineScope,
    protected val intentState: IntentState,
    private val capabilitiesService: CapabilitiesService,
) : WalletNavigationController {

    // Holds the route intercepted by a prerequisite gate. navigatePending() resumes to it.
    //
    // Backed by MutableStateFlow rather than a plain var: two rapid
    // navigate(PrerequisiteRoute) calls can both suspend at evaluatePrerequisites() and then
    // resume in either order on K/Native, where a plain var has no cross-thread visibility
    // guarantee. MutableStateFlow ensures all writes are immediately visible to any reader.
    //
    // The overwrite warning surfaces the residual race: even with visibility fixed, the second
    // call's route will silently replace the first's if both fail the prerequisites check
    // concurrently. If that ever becomes a problem, this field should become a queue.
    private val _pendingRoute = MutableStateFlow<Route?>(null)
    var pendingRoute: Route?
        get() = _pendingRoute.value
        set(value) {
            if (value != null && _pendingRoute.value != null) {
                Napier.w("pendingRoute overwritten: ${_pendingRoute.value} → $value")
            }
            _pendingRoute.value = value
        }

    override fun navigate(route: Route) {
        scope.launch {
            when (route) {
                is PrerequisiteRoute -> {
                    when (capabilitiesService.evaluatePrerequisites(route.prerequisites).first()) {
                        true -> navController.navigateOnMain(route)
                        false -> {
                            pendingRoute = route
                            navController.navigateOnMain(CapabilitiesRoute(route.prerequisites))
                        }
                    }
                }
                else -> {
                    Napier.d("Navigate to: $route")
                    navController.navigateOnMain(route)
                }
            }
        }
    }

    override fun navigateBack() {
        scope.launch {
            Napier.d("Navigate back")
            if (!navController.navigateUpOnMain()) {
                popToInvoker()
            }
        }
    }

    override fun popBackStack(route: Route) {
        scope.launch {
            Napier.d("popBackStack: $route")
            navController.popBackStackOnMain(route)
        }
    }

    override fun navigateNewGraph(route: Route) {
        scope.launch {
            Napier.d("navigateNewGraph: $route")
            navController.navigateOnMain(route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    override fun navigatePending() {
        scope.launch {
            pendingRoute?.let {
                Napier.d("Replace current with $it")
                if (navController.replaceCurrentOnMain(it)) {
                    pendingRoute = null
                } else {
                    navigateBack()
                }
            } ?: navigateBack()
        }
    }

    override fun invocationAwareBack() {
        if (shouldFinishToCaller()) {
            intentState.finishApp?.invoke() ?: navigateBack()
        } else {
            navigateBack()
        }
    }

    override fun popToInvoker() {
        scope.launch {
            if (hasHomeScreenInBackStack()) {
                Napier.d("popBackStack: HomeScreenRoute")
                navController.popBackStackOnMain(HomeScreenRoute)
            } else {
                Napier.d("navigateNewGraph: HomeScreenRoute")
                navController.navigateOnMain(HomeScreenRoute) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    override fun shouldFinishToCaller(): Boolean =
        intentState.dcapiInvocationData.value != null

    private fun hasHomeScreenInBackStack(): Boolean {
        val route = HomeScreenRoute::class.qualifiedName
        return try {
            navController.getBackStackEntry(route!!)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}

// Overrides methods that differ in the transient external flow (no wallet home screen):
// - invocationAwareBack() always finishes to the caller instead of checking shouldFinishToCaller()
// - navigateBack() calls finishApp when the back stack is exhausted
// - popToInvoker() always prefers finishApp
internal class TransientFlowNavigationControllerImpl(
    navController: NavHostController,
    scope: CoroutineScope,
    intentState: IntentState,
    capabilitiesService: CapabilitiesService,
) : WalletNavigationControllerImpl(navController, scope, intentState, capabilitiesService) {

    override fun invocationAwareBack() {
        intentState.finishApp?.invoke() ?: navigateBack()
    }

    override fun navigateBack() {
        scope.launch {
            if (!navController.navigateUpOnMain()) {
                intentState.finishApp?.invoke()
            }
        }
    }

    override fun popToInvoker() {
        intentState.finishApp?.invoke() ?: navigateBack()
    }
}
