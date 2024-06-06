import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter

@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    val context = LocalContext.current
    return AndroidPlatformAdapter(context)
}

actual fun getObjectFactory(): ObjectFactory {
    return AndroidObjectFactory()
}