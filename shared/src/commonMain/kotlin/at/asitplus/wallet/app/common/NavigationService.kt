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

    fun navigate(route: Route) {
        CoroutineScope(Dispatchers.Main).launch {
            navigate.emit(route)
        }
    }

    fun navigateBack() {
        CoroutineScope(Dispatchers.Main).launch {
            navigateBack.emit(null)
        }
    }

    fun popBackStack(route: Route) {
        CoroutineScope(Dispatchers.Main).launch {
            popBackStack.emit(route)
        }
    }
}