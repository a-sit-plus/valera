import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain


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

internal object AppTestTags {
    const val rootScaffold = "rootScaffold"
}

@Composable
fun App(walletMain: WalletMain, uiProvider: UiProvider) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Throwable) {
        walletMain.errorService.emit(UncorrectableErrorException(e))
    }

    MaterialTheme(
        colorScheme = uiProvider.colorScheme,
    ) {
        CompositionLocalProvider(
            LocalUiProvider provides uiProvider,
        ) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }, modifier = Modifier.testTag(AppTestTags.rootScaffold)
            ) { _ ->
                Navigator(walletMain)
            }
        }
    }
}