package ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.navigation.routes.Route

suspend fun NavHostController.navigateOnMain(route: Route) {
    withContext(Dispatchers.Main.immediate) {
        navigate(route)
    }
}

suspend fun NavHostController.navigateOnMain(
    route: Route,
    builder: NavOptionsBuilder.() -> Unit
) {
    withContext(Dispatchers.Main.immediate) {
        navigate(route, builder)
    }
}

suspend fun NavHostController.replaceCurrentOnMain(route: Route): Boolean = withContext(Dispatchers.Main.immediate) {
    val currentDestinationId = currentDestination?.id ?: return@withContext false
    navigate(route) {
        popUpTo(currentDestinationId) { inclusive = true }
        launchSingleTop = true
    }
    true
}

suspend fun NavHostController.navigateUpOnMain(): Boolean = withContext(Dispatchers.Main.immediate) {
    navigateUp()
}

suspend fun NavHostController.popBackStackOnMain(route: Route): Boolean = withContext(Dispatchers.Main.immediate) {
    popBackStack(route = route, inclusive = false)
}
