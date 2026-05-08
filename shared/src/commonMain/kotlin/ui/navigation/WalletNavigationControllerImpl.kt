package ui.navigation

import androidx.navigation.NavHostController
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.IntentState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
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
    // Plain var is safe because pendingRoute is only written/read imperatively (never observed by Compose).
    var pendingRoute: Route? = null

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
                Napier.w("Navigate up failed")
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
                popUpTo(0)
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

    override fun returnToHome() {
        scope.launch {
            if (hasHomeScreenInBackStack()) {
                Napier.d("popBackStack: HomeScreenRoute")
                navController.popBackStackOnMain(HomeScreenRoute)
            } else {
                Napier.d("navigateNewGraph: HomeScreenRoute")
                navController.navigateOnMain(HomeScreenRoute) {
                    popUpTo(0)
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

// Overrides the two methods that differ in the sharing/DCAPI flow:
// - navigateBack() calls finishApp when the back stack is exhausted (no wallet home to fall back to)
// - returnToHome() always prefers finishApp since there is no wallet home screen in this graph
// - shouldFinishToCaller() also checks iosDcApiPreRequestData (pre-request stage, not yet an invocation)
internal class SharingNavigationControllerImpl(
    navController: NavHostController,
    scope: CoroutineScope,
    intentState: IntentState,
    capabilitiesService: CapabilitiesService,
) : WalletNavigationControllerImpl(navController, scope, intentState, capabilitiesService) {

    override fun navigateBack() {
        scope.launch {
            if (!navController.navigateUpOnMain()) {
                intentState.finishApp?.invoke()
            }
        }
    }

    override fun returnToHome() {
        intentState.finishApp?.invoke() ?: navigateBack()
    }

    override fun shouldFinishToCaller(): Boolean =
        intentState.iosDcApiPreRequestData.value != null || intentState.dcapiInvocationData.value != null
}