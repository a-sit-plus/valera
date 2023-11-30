
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain

actual fun getPlatformName(): String = "iOS"

@Composable
actual fun getColorScheme(): ColorScheme{
    return if(isSystemInDarkTheme()){
        darkColorScheme()
    } else {
        lightColorScheme()
    }
}
actual fun decodeImage(image: ByteArray): ImageBitmap {
    TODO("Return ImageBitmap from JPEG ByteArray")
}



fun MainViewController(objectFactory: ObjectFactory, platformAdapter: PlatformAdapter) = ComposeUIViewController {
    App(WalletMain(objectFactory, DataStoreService(createDataStore()), platformAdapter))
}