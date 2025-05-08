import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal val testHttpClient by lazy {
    HttpClient {
        configureForPlatform()
    }
}

internal expect fun HttpClientConfig<*>.configureForPlatform()