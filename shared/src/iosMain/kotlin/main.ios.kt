
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual fun getPlatformName(): String = "iOS"

@Composable
actual fun getColorScheme(): ColorScheme{
    return if(isSystemInDarkTheme()){
        darkColorScheme()
    } else {
        lightColorScheme()
    }
}

fun MainViewController(objectFactory: ObjectFactory) = ComposeUIViewController {
    App(WalletMain(objectFactory, DataStoreService(createDataStore())))
}


/**
 * Workaround to check when the Compose App gets opened in the event of an associated domain.
 */
actual fun pollAppLink(url: MutableState<String?>, walletMain: WalletMain){
    CoroutineScope(Dispatchers.Default).launch {
        while (true){
            delay(250)
            if (walletMain.objectFactory.appLink != null){
                url.value = walletMain.objectFactory.appLink
            }
        }
    }
}