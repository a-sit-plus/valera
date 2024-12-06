import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.app.common.PlatformAdapter
import io.ktor.utils.io.errors.IOException

@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    val context = LocalContext.current
    return AndroidPlatformAdapter(context)
}

