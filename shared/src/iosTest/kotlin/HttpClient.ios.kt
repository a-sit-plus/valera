import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun HttpClientConfig<*>.configureForPlatform() {

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    engine{
        this as DarwinClientEngineConfig

        handleChallenge { session, task, challenge, completionHandler ->
            ServerTrustHandlerTest().handleChallenge(
                session,
                task,
                challenge,
                completionHandler
            )
        }

    }
}