
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import ui.theme.WalletTheme


/**
 * Global variable to utilize back navigation functionality
 */
var globalBack: () -> Unit = {}

/**
 * Global variable which especially helps to channel information from swift code
 * to compose whenever the app gets called via an associated domain
 */
var appLink = mutableStateOf<String?>(null)

/**
 * Global variable to test at least something from the iOS UITest
 */
var iosTestValue = Configuration.IOS_TEST_VALUE

@Composable
fun App(walletMain: WalletMain) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Throwable){
        walletMain.errorService.emit(UncorrectableErrorException(e))
    }

    WalletTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { _ ->
            Navigator(walletMain)
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme