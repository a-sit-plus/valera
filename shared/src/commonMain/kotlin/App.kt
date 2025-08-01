import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.*
import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.common.di.appModule
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import ui.navigation.WalletNavigation
import ui.theme.WalletTheme
import ui.viewmodels.authentication.PresentationStateModel

/**
 * Global variables which help to channel information from platform-specific code
 * to compose whenever the app gets called from native code, such as via an associated domain,
 * NFC or the DC API
 */
object Globals {
    var appLink = MutableStateFlow<String?>(null)
    var dcapiInvocationData = MutableStateFlow<DCAPIInvocationData?>(null)
    var presentationStateModel = MutableStateFlow<PresentationStateModel?>(null)
}

internal object AppTestTags {
    const val rootScaffold = "rootScaffold"
}

@Composable
fun App(walletDependencyProvider: WalletDependencyProvider) {
    KoinApplication({
        modules(appModule(walletDependencyProvider))
    }) {
        val koinScope = koinInject<SessionService>().scope.collectAsState().value
        catchingUnwrapped {
            val walletMain: WalletMain = koinInject(scope = koinScope)

            LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
                Napier.d("Lifecycle.Event.ON_CREATE")
                walletMain.updateCheck()
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                Napier.d("Lifecycle.Event.ON_RESUME")
            }

        }.onFailure {
            val errorService: ErrorService = koinInject(scope = koinScope)
            errorService.emit(it)
        }

        WalletTheme {
            WalletNavigation(koinScope = koinScope)
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme

expect fun getImageDecoder(image: ByteArray): ImageBitmap