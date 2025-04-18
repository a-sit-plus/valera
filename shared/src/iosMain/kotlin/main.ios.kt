import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.WalletMain
import data.storage.RealDataStoreService
import data.storage.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Image
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

actual fun getImageDecoder(image: ByteArray): ImageBitmap {
    return Image.makeFromEncoded(image).toComposeImageBitmap()
}


fun MainViewController(
    platformAdapter: PlatformAdapter,
    buildContext: BuildContext,
): UIViewController {
    val dataStoreService = RealDataStoreService(createDataStore(), platformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    return ComposeUIViewController {
        App(
            WalletMain(
                WalletKeyMaterial(keyMaterial = keystoreService.getSignerBlocking()),
                dataStoreService,
                platformAdapter,
                buildContext =  buildContext
            )
        )
    }
}
