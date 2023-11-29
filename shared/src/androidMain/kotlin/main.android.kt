
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import at.asitplus.KmmResult
import at.asitplus.wallet.app.android.AndroidCryptoService
import at.asitplus.wallet.app.android.AndroidKeyStoreService
import at.asitplus.wallet.app.common.HolderKeyService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier


actual fun openUrl(url: String, objectFactory: ObjectFactory){
    println("Open URL: ${url.toUri()}")
    val androidObjectFactory = objectFactory as AndroidObjectFactory
    val context = androidObjectFactory.context
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))

}

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

@Composable
fun MainView() {
    App(WalletMain(objectFactory = AndroidObjectFactory(LocalContext.current), DataStoreService(getDataStore(LocalContext.current))))
}

class AndroidObjectFactory(val context: Context) : ObjectFactory {
    val keyStoreService: AndroidKeyStoreService by lazy { AndroidKeyStoreService() }

    init {
        Napier.base(DebugAntilog())
    }

    override fun loadCryptoService(): KmmResult<CryptoService> {
        val keyPair = keyStoreService.loadKeyPair()
            ?: return KmmResult.failure(Throwable("Could not create key pair"))
        val certificate = keyStoreService.loadCertificate()
            ?: return KmmResult.failure(Throwable("Could not load certificate"))
        val cryptoService = AndroidCryptoService(keyPair, certificate)
        return KmmResult.success(cryptoService)
    }

    override fun loadHolderKeyService(): KmmResult<HolderKeyService> {
        return KmmResult.success(keyStoreService)
    }
}