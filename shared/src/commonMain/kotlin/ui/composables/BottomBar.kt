package ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_sign
import at.asitplus.valera.resources.navigation_button_label_my_data
import at.asitplus.valera.resources.navigation_button_label_show_data
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.navigation.routes.AuthenticationQrCodeScannerRoute
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SigningRoute

@Composable
fun BottomBar(navigate: (Route) -> Unit, selected: NavigationData) {
    NavigationBar {
        for (route in listOf(
            NavigationData.HOME_SCREEN,
            NavigationData.AUTHENTICATION_SCANNING_SCREEN,
            NavigationData.SIGNING_SCREEN,
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
        isActive = {
            when (it) {
                is HomeScreenRoute -> true
                else -> false
            }
        },
    ),
    AUTHENTICATION_SCANNING_SCREEN(
        title = Res.string.navigation_button_label_show_data,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
        },
        destination = AuthenticationQrCodeScannerRoute,
        isActive = {
            when (it) {
                is AuthenticationQrCodeScannerRoute -> true
                else -> false
            }
        },
    ),
    SIGNING_SCREEN(
        title = Res.string.button_label_sign,
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
            )
        },
        destination = SigningRoute,
        isActive = {
            when (it) {
                is SigningRoute -> true
                else -> false
            }
        },
    ),
}