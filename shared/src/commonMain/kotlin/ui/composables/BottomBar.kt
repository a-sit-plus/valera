package ui.composables

import NavigationData
import Route
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomBar(navigate: (Route) -> Unit, selected: NavigationData) {
    NavigationBar {
        for (route in listOf(
            NavigationData.HOME_SCREEN,
            NavigationData.AUTHENTICATION_SCANNING_SCREEN,
            NavigationData.INFORMATION_SCREEN,
        )) {
            NavigationBarItem(
                icon = route.icon,
                label = {
                    Text(stringResource(route.title))
                },
                onClick = {
                    if (selected.destination != route.destination) {
                        navigate(route.destination)
                    }
                },
                selected = selected == route,
            )
        }
    }
}