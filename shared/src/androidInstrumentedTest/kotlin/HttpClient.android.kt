import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun HttpClientConfig<*>.configureForPlatform() {

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}