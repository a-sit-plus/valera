package at.asitplus.wallet.app.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ui.navigation.routes.Route

class NavigationService {
    val navigate = MutableSharedFlow<NavigationFlowData>()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun navigate(route: Route) {
        scope.launch {
            navigate.emit(NavigationFlowData(route, NavigationEnum.Navigate))
        }
    }

    fun navigateBack() {
        scope.launch {
            navigate.emit(NavigationFlowData(null, NavigationEnum.NavigateBack))
        }
    }

    fun popBackStack(route: Route) {
        scope.launch {
            navigate.emit(NavigationFlowData(route, NavigationEnum.PopBackStack))
        }
    }
}
data class NavigationFlowData(val route: Route?, val method: NavigationEnum)

enum class NavigationEnum {
    Navigate,
    NavigateBack,
    PopBackStack
}