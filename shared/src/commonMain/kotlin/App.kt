import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module
import ui.navigation.WalletNavigation
import ui.theme.WalletTheme

internal object AppTestTags {
    const val rootScaffold = "rootScaffold"
}

@ExperimentalMaterial3Api
@Composable
fun App(
    koinModule: Module,
    intentState: IntentState
) {
    KoinApplication({
        modules(koinModule)
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
//            Scaffold {
//                Box(modifier = Modifier.padding(it)) {
//                    Box(
//                        modifier = Modifier.align(Alignment.Center)
//                    ) {
//                        CredentialSetQueryOptionSelectionCardPreview()
//                    }
//                }
//            }
            WalletNavigation(
                koinScope = koinScope,
                intentState = intentState
            )
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme
