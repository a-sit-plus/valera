import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import data.storage.RealDataStoreService
import data.storage.createDataStore
import platform.UIKit.UIViewController
import ui.composables.IosBiometryPrompt
import ui.navigation.NavigationPages
import ui.theme.darkScheme
import ui.theme.lightScheme
import ui.views.IosCameraView

@Composable
fun getColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}

@Composable
fun IosApp(
    walletMain: WalletMain,
) {
    App(
        walletMain,
        UiProvider(
            colorScheme = getColorScheme(),
            cameraView = IosCameraView,
            biometryPrompt = IosBiometryPrompt,
            navigationPages = NavigationPages.createWithDefaults(),
        ),
    )
}

fun MainViewController(
    objectFactory: ObjectFactory,
    platformAdapter: PlatformAdapter,
    buildContext: BuildContext,
): UIViewController = ComposeUIViewController {
    IosApp(
        WalletMain.createWithDefaults(
            objectFactory = objectFactory,
            dataStoreService = RealDataStoreService(createDataStore(), platformAdapter),
            platformAdapter = platformAdapter,
            buildContext = buildContext,
        )
    )
}
