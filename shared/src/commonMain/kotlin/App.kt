import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.common.di.appModule
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import ui.navigation.WalletNavigation
import ui.theme.WalletTheme
import ui.viewmodels.ErrorViewModel
import ui.viewmodels.authentication.PresentationStateModel
import ui.views.ErrorView

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
fun App(walletDependencyProvider: Result<WalletDependencyProvider>) {
    walletDependencyProvider.onSuccess {
        KoinApplication({
            modules(appModule(it))
        }) {
            catchingUnwrapped {
                val walletMain: WalletMain = koinInject()

                LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
                    Napier.d("Lifecycle.Event.ON_CREATE")
                    walletMain.updateCheck()
                }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    Napier.d("Lifecycle.Event.ON_RESUME")
                }

            }.onFailure {
                val errorService: ErrorService = koinInject()
                errorService.emit(it)
            }

            WalletTheme {
                WalletNavigation()
            }
        }
    }.onFailure {
        WalletTheme {
            ErrorView(vm = ErrorViewModel(resetStack = {}, resetApp =  {}, throwable =  Throwable(""), onClickLogo = {}, {}))
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme

expect fun getImageDecoder(image: ByteArray): ImageBitmap
