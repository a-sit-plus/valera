package ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.navigation_button_label_check
import at.asitplus.valera.resources.navigation_button_label_my_data
import at.asitplus.valera.resources.navigation_button_label_settings
import at.asitplus.valera.resources.navigation_button_label_show_data
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.navigation.Routes.VerifyDataRoute
import ui.navigation.Routes.HomeScreenRoute
import ui.navigation.Routes.Route
import ui.navigation.Routes.SettingsRoute
import ui.navigation.Routes.ShowDataRoute

@Composable
fun BottomBar(navigate: (Route) -> Unit, selected: NavigationData) {
    NavigationBar {
        for (route in listOf(
            NavigationData.HOME_SCREEN,
            NavigationData.SHOW_DATA_SCREEN,
            NavigationData.VERIFY_DATA_SCREEN,
            NavigationData.INFORMATION_SCREEN,
        )) {
            NavigationBarItem(
                icon = route.icon,
                label = { Text(stringResource(route.title)) },
                onClick = {
                    if (selected.destination != route.destination) {
                        navigate(route.destination)
                    }
                },
                selected = selected == route
            )
        }
    }
}

enum class NavigationData(
    val title: StringResource,
    val icon: @Composable () -> Unit,
    val destination: Route,
    val isActive: (Route) -> Boolean
) {
    HOME_SCREEN(
        title = Res.string.navigation_button_label_my_data,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
            )
        },
        destination = HomeScreenRoute,
        isActive = { it is HomeScreenRoute },
    ),
    SHOW_DATA_SCREEN(
        title = Res.string.navigation_button_label_show_data,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
        },
        destination = ShowDataRoute,
        isActive = { it is ShowDataRoute }
    ),
    VERIFY_DATA_SCREEN(
        title = Res.string.navigation_button_label_check,
        icon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null
            )
        },
        destination = VerifyDataRoute,
        isActive = { it is VerifyDataRoute }
    ),
    INFORMATION_SCREEN(
        title = Res.string.navigation_button_label_settings,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
            )
        },
        destination = SettingsRoute,
        isActive = { it is SettingsRoute }
    )
}
