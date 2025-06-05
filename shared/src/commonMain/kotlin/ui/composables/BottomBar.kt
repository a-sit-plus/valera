package ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_sign
import at.asitplus.valera.resources.navigation_button_label_check
import at.asitplus.valera.resources.navigation_button_label_my_data
import at.asitplus.valera.resources.navigation_button_label_show_data
import at.asitplus.wallet.lib.data.vckJsonSerializer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.PresentDataRoute
import ui.navigation.routes.QrCodeScannerRoute
import ui.navigation.routes.Route
import ui.navigation.routes.VerifyDataRoute
import ui.viewmodels.QrCodeScannerMode

@Composable
fun BottomBar(navigate: (Route) -> Unit, selected: NavigationData) {
    NavigationBar {
        for (route in listOf(
            NavigationData.HOME_SCREEN,
            NavigationData.PRESENT_DATA_SCREEN,
            NavigationData.VERIFY_DATA_SCREEN,
            NavigationData.SIGNING_SCREEN,
        )) {
            NavigationBarItem(
                icon = route.icon,
                label = {
                    Text(
                        text = stringResource(route.title),
                        textAlign = TextAlign.Center,
                    )
                },
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
    PRESENT_DATA_SCREEN(
        title = Res.string.navigation_button_label_show_data,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
        },
        destination = PresentDataRoute,
        isActive = { it is PresentDataRoute }
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
    SIGNING_SCREEN(
        title = Res.string.button_label_sign,
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
            )
        },
        destination = QrCodeScannerRoute(QrCodeScannerMode.SIGNING),
        isActive = {
            when (it) {
                is QrCodeScannerRoute -> true
                else -> false
            }
        },
    ),
}
