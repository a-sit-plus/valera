package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.oidvci.WalletService
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

const val HOST = "https://wallet.a-sit.at"

class ProvisioningService(val objectFactory: ObjectFactory, val dataStoreService: DataStoreService, val cryptoService: CryptoService) {
    var xauth: String? = null
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
        dataStoreService.setData(response.headers["X-Auth-Token"]!!, "xauth")
        if (urlToOpen != null) {
            return urlToOpen
        } else {
            throw Exception("Redirect not found in header")
        }
    }

    suspend fun step2(redirect: String){
        objectFactory.openUrl(redirect)
    }
    suspend fun step3(url: String){
        val xauth = dataStoreService.getData("xauth")
        println("Step3: create request with x-auth: $xauth")
        val response = client.get(url) {
            headers["X-Auth-Token"] = xauth!!
        }
        println("Step3 response: $response")
        println("Step3 header: ${response.headers}")
        dataStoreService.setData(response.headers["Set-Cookie"]!!, "setcookie")

        step4()
    }

    suspend fun step4(){
        val oid4vciService = WalletService(
            credentialScheme = at.asitplus.wallet.idaustria.ConstantIndex.IdAustriaCredential,
            clientId = "https://eid.a-sit.at/wallet",
            cryptoService = cryptoService,
            credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT
        )
        val authRequest = oid4vciService.createAuthRequest()
        println("authRequest: $authRequest")
    }
}