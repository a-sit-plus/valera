import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig


internal val httpClient by lazy {
    HttpClient {
        configure()
    }
}

internal fun HttpClientConfig<*>.configure() {
    configureForPlatform()
}

internal expect fun HttpClientConfig<*>.configureForPlatform()