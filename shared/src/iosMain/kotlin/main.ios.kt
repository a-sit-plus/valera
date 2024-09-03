import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.SignerKeyPairAdapter
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.app.common.WalletMain
import data.storage.RealDataStoreService
import data.storage.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    platformAdapter: PlatformAdapter,
    buildContext: BuildContext,
): UIViewController {
    val dataStoreService = RealDataStoreService(createDataStore(), platformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    return ComposeUIViewController {
        App(
            WalletMain(
                WalletCryptoService(keyPairAdapter= SignerKeyPairAdapter(signerWithCert= keystoreService.getSignerBlocking())),
                 keystoreService,
                dataStoreService,
                platformAdapter,
                buildContext =  buildContext,
                scope = CoroutineScope(Dispatchers.Default)
            )
        )
    }
}
