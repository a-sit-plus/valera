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
import ui.theme.darkScheme
import ui.theme.lightScheme

actual fun getPlatformName(): String = "iOS"

@Composable
actual fun getColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}


fun MainViewController(
    objectFactory: ObjectFactory,
    platformAdapter: PlatformAdapter,
    buildContext: BuildContext,
): UIViewController = ComposeUIViewController {
    App(
        WalletMain(
            objectFactory = objectFactory,
            RealDataStoreService(createDataStore(), platformAdapter),
            platformAdapter = platformAdapter,
            buildContext = buildContext,
        )
    )
}
