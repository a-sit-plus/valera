import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService

actual fun getPlatformName(): String = "Android"

// Modified from https://developer.android.com/jetpack/compose/designsystems/material3
@Composable
actual fun getColorScheme(): ColorScheme{
    // Dynamic color is available on Android 12+
    val darkColorScheme = darkColorScheme()
    val lightColorScheme = lightColorScheme()
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val darkTheme = isSystemInDarkTheme()
    return when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme (LocalContext.current)
                dynamicColor && !darkTheme -> dynamicLightColorScheme (LocalContext.current)

        darkTheme -> darkColorScheme
                else -> lightColorScheme
    }
}

@Composable fun MainView() = App(WalletMain(objectFactory = object : ObjectFactory{
    override suspend fun loadCryptoService(): KmmResult<CryptoService> {
        return KmmResult.success(DefaultCryptoService())
    }
}))
