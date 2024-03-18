import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import at.asitplus.KmmResult
import at.asitplus.wallet.app.android.AndroidCryptoService
import at.asitplus.wallet.app.android.AndroidKeyStoreService
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.HolderKeyService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import ui.theme.darkScheme
import ui.theme.lightScheme
import java.io.File

actual fun getPlatformName(): String = "Android"

// Modified from https://developer.android.com/jetpack/compose/designsystems/material3
@Composable
actual fun getColorScheme(): ColorScheme {
    // Dynamic color is available on Android 12+, but let's use our color scheme for branding
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}

@Composable
fun MainView(buildContext: BuildContext) {
    val platformAdapter = AndroidPlatformAdapter(LocalContext.current)

    App(
        WalletMain(
            objectFactory = AndroidObjectFactory(),
            RealDataStoreService(getDataStore(LocalContext.current), platformAdapter),
            platformAdapter = platformAdapter,
            buildContext = buildContext,
        )
    )
}

class AndroidObjectFactory : ObjectFactory {
    val keyStoreService: AndroidKeyStoreService by lazy { AndroidKeyStoreService() }

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

class AndroidPlatformAdapter(val context: Context) : PlatformAdapter {
    override fun openUrl(url: String) {
        Napier.d("Open URL: ${url.toUri()}")
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        return bitmap.asImageBitmap()
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        val file = File(folder, fileName)
        if (file.exists()) {
            file.appendText(text)
        } else {
            file.createNewFile()
            file.writeText(text)
        }
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        val file = File(folder, fileName)
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    override fun clearFile(fileName: String, folderName: String) {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        val file = File(folder, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun exitApp() {
        Napier.d("Exit App gracefully")
        val activity = context as Activity
        activity.finish()
    }

    override fun shareLog() {
        val folder = File(context.filesDir, "logs")
        val file = File(folder, "log.txt")
        val fileUri = FileProvider.getUriForFile(
            context,
            "at.asitplus.wallet.app.android.fileprovider",
            file
        )


        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/text"
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}