
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
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


    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
        Napier.d("Lifecycle.Event.ON_CREATE")
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        Napier.d("Lifecycle.Event.ON_RESUME")
    }

    WalletTheme {
        WalletNavigation(walletMain)
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme