package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

/**
 * Central place to build [HttpClient]
 */
class HttpService(private val buildContext: BuildContext) {
    fun buildHttpClient(cookieStorage: CookiesStorage? = null) = HttpClient {
        followRedirects = false
        install(ContentNegotiation) {
            json(vckJsonSerializer)
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        apply(loggingConfig)
        install(HttpCookies) {
            cookieStorage?.let {
                storage = it
            }
        }
    }

    val loggingConfig: HttpClientConfig<*>.() -> Unit
        get() = {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.i(message, tag = "HTTP")
                    }
                }
                level = LogLevel.ALL
            }
            install(DefaultRequest) {
                header(HttpHeaders.UserAgent, "Valera/${buildContext.versionName} (${buildContext.osVersion})")
            }
        }
}