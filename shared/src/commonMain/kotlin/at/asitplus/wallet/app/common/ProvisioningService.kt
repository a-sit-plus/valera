package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.oidc.OpenIdConstants.TOKEN_PREFIX_BEARER
import at.asitplus.wallet.lib.oidvci.CredentialResponseParameters
import at.asitplus.wallet.lib.oidvci.IssuerMetadata
import at.asitplus.wallet.lib.oidvci.TokenResponseParameters
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json

const val HOST = "https://wallet.a-sit.at"
const val PATH_WELL_KNOWN_CREDENTIAL_ISSUER = "/.well-known/openid-credential-issuer"

class ProvisioningService(val objectFactory: ObjectFactory, val dataStoreService: DataStoreService, val cryptoService: CryptoService) {
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
        println("step1: HTTP.GET $HOST/m1/oauth2/authorization/idaq")
        val response = client.get("$HOST/m1/oauth2/authorization/idaq")
        val urlToOpen = response.headers["Location"]

        val xauth = response.headers["X-Auth-Token"]
        println("Store X-Auth-Token: $xauth")
        dataStoreService.setData(xauth!!, "xauth")

        if (urlToOpen != null) {
            return urlToOpen
        } else {
            throw Exception("Redirect not found in header")
        }
    }

    suspend fun step2(redirect: String){
        println("step2: Open URL: $redirect")
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
        val xauth = dataStoreService.getData("xauth")
        println("Load X-Auth-Token: $xauth")
        println("Step4: HTTP:GET https://eid.a-sit.at/wallet$PATH_WELL_KNOWN_CREDENTIAL_ISSUER")
        val metadata: IssuerMetadata = client.get("https://eid.a-sit.at/wallet$PATH_WELL_KNOWN_CREDENTIAL_ISSUER"){
            headers["X-Auth-Token"] = xauth!!
        }.body()

        val oid4vciService = WalletService(
            credentialScheme = at.asitplus.wallet.idaustria.ConstantIndex.IdAustriaCredential,
            clientId = "https://eid.a-sit.at/wallet",
            cryptoService = cryptoService,
            credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT
        )

        println("Step4: oid4vciService.createAuthRequest()")
        val authRequest = oid4vciService.createAuthRequest()

        println("Step4: client.get(${metadata.authorizationEndpointUrl})")

        val codeUrl = client.get(metadata.authorizationEndpointUrl) {
            authRequest.encodeToParameters().forEach {
                println("authRequest.encodeToParameters(): $it")
                this.parameter(it.key, it.value)
            }
            headers["X-Auth-Token"] = xauth!!
        }.headers[HttpHeaders.Location]

        if (codeUrl == null) {
            throw Exception("codeUrl is null")
        }
        val code = Url(codeUrl).parameters["code"]
        if (code == null) {
           throw Exception("code is null")
        }

        val tokenRequest = oid4vciService.createTokenRequestParameters(code)
        val tokenResponse: TokenResponseParameters = client.submitForm(metadata.tokenEndpointUrl!!) {
            setBody(tokenRequest.encodeToParameters().formUrlEncode())
        }.body()
        val credentialRequest = oid4vciService.createCredentialRequest(tokenResponse, metadata)
        val credentialResponse: CredentialResponseParameters =
            client.post(metadata.credentialEndpointUrl!!) {
                contentType(ContentType.Application.Json)
                setBody(credentialRequest)
                headers {
                    append(
                        HttpHeaders.Authorization,
                        "$TOKEN_PREFIX_BEARER${tokenResponse.accessToken}"
                    )
                }
            }.body()
        println(credentialResponse)

    }
}