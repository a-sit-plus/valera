import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.WalletMain
import data.storage.RealDataStoreService
import data.storage.createDataStore
import org.jetbrains.skia.Image
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.prompt.IosPromptModel
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
    val promptModel = IosPromptModel()

    return ComposeUIViewController {
        PromptDialogs(promptModel)
        App(
            WalletDependencyProvider(
                keystoreService,
                dataStoreService,
                platformAdapter,
                buildContext =  buildContext,
                promptModel = promptModel
            )
        )
    }
}