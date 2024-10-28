
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import ui.navigation.OnboardingNavigation
import ui.navigation.Routes.OnboardingWrapperTestTags
import ui.navigation.WalletNavigation
import ui.theme.WalletTheme

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
fun App(walletMain: WalletMain) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Throwable){
        walletMain.errorService.emit(UncorrectableErrorException(e))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
        Napier.d("Lifecycle.Event.ON_CREATE")
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        Napier.d("Lifecycle.Event.ON_RESUME")
    }

    val isConditionsAccepted by walletMain.walletConfig.isConditionsAccepted.collectAsState(null)

    WalletTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            modifier = Modifier.testTag(AppTestTags.rootScaffold)
        ) { _ ->
            when (isConditionsAccepted) {
                null -> {
                    Box(modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingLoadingIndicator))
                }
                true -> WalletNavigation(walletMain)
                false -> OnboardingNavigation(walletMain)
            }
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme