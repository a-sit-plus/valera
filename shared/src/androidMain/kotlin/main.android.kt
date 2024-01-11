
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import at.asitplus.KmmResult
import at.asitplus.wallet.app.android.AndroidCryptoService
import at.asitplus.wallet.app.android.AndroidKeyStoreService
import at.asitplus.wallet.app.common.HolderKeyService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

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
    App(WalletMain(objectFactory = AndroidObjectFactory(), DataStoreService(getDataStore(LocalContext.current)), platformAdapter = AndroidPlatformAdapter(LocalContext.current)))
}

class AndroidObjectFactory : ObjectFactory {
    val keyStoreService: AndroidKeyStoreService by lazy { AndroidKeyStoreService() }

    init {
        Napier.takeLogarithm()
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

class AndroidPlatformAdapter(val context: Context): PlatformAdapter{
    override fun openUrl(url: String) {
        Napier.d("Open URL: ${url.toUri()}")
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        return bitmap.asImageBitmap()
    }
}