package at.asitplus.wallet.app.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ui.navigation.routes.Route

class NavigationService {
    val navigate = MutableSharedFlow<Route>()
    val navigateBack = MutableSharedFlow<Route?>()
    val popBackStack = MutableSharedFlow<Route>()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun navigate(route: Route) {
        scope.launch {
            navigate.emit(route)
        }
    }

    fun navigateBack() {
        scope.launch {
            navigateBack.emit(null)
        }
    }

    fun popBackStack(route: Route) {
        scope.launch {
            popBackStack.emit(route)
        }
    }
}