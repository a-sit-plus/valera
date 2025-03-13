
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import ui.navigation.WalletNavigation
import ui.theme.WalletTheme

/**
 * Global variable which especially helps to channel information from swift code
 * to compose whenever the app gets called via an associated domain
 */
var appLink = MutableStateFlow<String?>(null)

internal object AppTestTags {
    const val rootScaffold = "rootScaffold"
}

@Composable
fun App(walletMain: WalletMain) {

    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
        Napier.d("Lifecycle.Event.ON_CREATE")
        walletMain.updateCheck()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        Napier.d("Lifecycle.Event.ON_RESUME")
        // TODO is this the best place to sync the credentials with the system?
        walletMain.updateDigitalCredentialsAPIIntegration()
    }

    WalletTheme {
        WalletNavigation(walletMain)
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme

expect fun getImageDecoder(image: ByteArray): ImageBitmap
