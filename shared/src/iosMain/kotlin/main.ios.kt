import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import data.storage.RealDataStoreService
import data.storage.createDataStore

actual fun getPlatformName(): String = "iOS"

@Composable
actual fun getColorScheme(): ColorScheme{
    return if(isSystemInDarkTheme()){
        darkColorScheme()
    } else {
        lightColorScheme()
    }
}


fun MainViewController(objectFactory: ObjectFactory, platformAdapter: PlatformAdapter) = ComposeUIViewController {
    App(WalletMain(objectFactory, RealDataStoreService(createDataStore()), platformAdapter))
}
