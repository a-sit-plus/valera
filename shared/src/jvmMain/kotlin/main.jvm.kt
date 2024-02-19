import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import data.storage.RealDataStoreService
import data.storage.createDataStore

actual fun getPlatformName(): String = "Desktop"

// Modified from https://developer.android.com/jetpack/compose/designsystems/material3
@Composable
actual fun getColorScheme(): ColorScheme {
    // Dynamic color is available on Android 12+
    val darkColorScheme = darkColorScheme()
    val lightColorScheme = lightColorScheme()
    val darkTheme = isSystemInDarkTheme()
    return when {
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }
}

@Composable fun MainView(objectFactory: ObjectFactory, platformAdapter: PlatformAdapter) = App(
    walletMain = WalletMain(
        objectFactory = objectFactory,
        platformAdapter = platformAdapter,
        dataStoreService = RealDataStoreService(createDataStore()),
    )
)