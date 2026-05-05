@file:OptIn(org.koin.core.annotation.KoinInternalApi::class)

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import org.koin.compose.ComposeContextWrapper
import org.koin.compose.LocalKoinScope
import org.koin.compose.koinInject
import ui.navigation.SharingNavigation
import ui.navigation.WalletNavigation
import ui.theme.WalletTheme

internal object AppTestTags {
    const val rootScaffold = "rootScaffold"
}

@ExperimentalMaterial3Api
@Composable
fun App(
    sessionService: SessionService,
    intentState: IntentState
) {
    val koinScope = sessionService.scope.collectAsState().value

    CompositionLocalProvider(
        LocalKoinScope provides ComposeContextWrapper(koinScope) { koinScope }
    ) {
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
            WalletNavigation(
                koinScope = koinScope,
                intentState = intentState
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun SharingApp(
    sessionService: SessionService,
    intentState: IntentState
) {
    val koinScope = sessionService.scope.collectAsState().value

    CompositionLocalProvider(
        LocalKoinScope provides ComposeContextWrapper(koinScope) { koinScope }
    ) {
        WalletTheme {
            SharingNavigation(
                koinScope = koinScope,
                intentState = intentState
            )
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme
