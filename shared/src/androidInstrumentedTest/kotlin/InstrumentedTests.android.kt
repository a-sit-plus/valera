import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.app.common.PlatformAdapter
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    val context = LocalContext.current
    return AndroidPlatformAdapter(context, {})
}

internal actual fun HttpClientConfig<*>.configureForPlatform() {

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}