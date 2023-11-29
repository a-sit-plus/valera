package at.asitplus.wallet.app.common

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import openUrl

const val HOST = "https://wallet.a-sit.at"

class ProvisioningService(val objectFactory: ObjectFactory) {
    private  val cookieStorage = AcceptAllCookiesStorage() // TODO: change to persistent cookie storage
    private val client = HttpClient {
        followRedirects = false
        install(ContentNegotiation) {
            json()
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(HttpCookies) {
            storage = cookieStorage
        }
    }
    suspend fun step1(): String{
        val response = client.get("$HOST/m1/oauth2/authorization/idaq")
        val urlToOpen = response.headers["Location"]
        if (urlToOpen != null) {
            return urlToOpen
        } else {
            throw Exception("Redirect not found in header")
        }
    }

    suspend fun step2(redirect: String){
        openUrl(redirect, objectFactory)
    }
    suspend fun step3(url: String){
        val response = client.get(url)
        println("Step3 response: $response")
        println("Step3 header: ${response.headers}")
    }
}