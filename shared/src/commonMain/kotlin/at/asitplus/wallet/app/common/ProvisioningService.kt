package at.asitplus.wallet.app.common

import Resources
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.oidc.OpenIdConstants.TOKEN_PREFIX_BEARER
import at.asitplus.wallet.lib.oidvci.CredentialFormatEnum
import at.asitplus.wallet.lib.oidvci.CredentialResponseParameters
import at.asitplus.wallet.lib.oidvci.IssuerMetadata
import at.asitplus.wallet.lib.oidvci.TokenResponseParameters
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import io.ktor.serialization.kotlinx.json.json

const val PATH_WELL_KNOWN_CREDENTIAL_ISSUER = "/.well-known/openid-credential-issuer"

class ProvisioningService(val platformAdapter: PlatformAdapter, val dataStoreService: DataStoreService, val cryptoService: CryptoService, val holderAgent: HolderAgent, val config: WalletConfig) {

    private val cookieStorage = PersistentCookieStorage(dataStoreService)
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
    @Throws(Throwable::class)
    suspend fun startProvisioning(){
        val host = config.host
        cookieStorage.reset()
        Napier.d("Start provisioning")

        runCatching { client.get("$host/m1/oauth2/authorization/idaq")
        }.onSuccess { response ->
            val urlToOpen = response.headers["Location"]

            val xAuthToken = response.headers["X-Auth-Token"]
            if (xAuthToken == null){
                throw Exception("X-Auth-Token not received")
            }

            Napier.d("Store X-Auth-Token: $xAuthToken")
            dataStoreService.setData(xAuthToken, Resources.DATASTORE_KEY_XAUTH)

            if (urlToOpen != null) {
                Napier.d("Open URL: $urlToOpen")
                platformAdapter.openUrl(urlToOpen)
            } else {
                throw Exception("Redirect not found in header")
            }
        }.onFailure {
            throw Exception(it)
        }
    }
    @Throws(Throwable::class)
    suspend fun handleResponse(url: String){
        val host = config.host
        val xAuthToken = dataStoreService.getData(Resources.DATASTORE_KEY_XAUTH)
        if (xAuthToken == null){
            throw Exception("X-Auth-Token not available in DataStoreService")
        }
        Napier.d("Create request with x-auth: $xAuthToken")
        client.get(url) {
            headers["X-Auth-Token"] = xAuthToken
        }

        Napier.d("Load X-Auth-Token: $xAuthToken")
        val metadata: IssuerMetadata = client.get("$host/m1$PATH_WELL_KNOWN_CREDENTIAL_ISSUER"){
            headers["X-Auth-Token"] = xAuthToken
        }.body()

        val oid4vciService = WalletService(
            credentialScheme = at.asitplus.wallet.idaustria.IdAustriaScheme,
            clientId = "$host/m1",
            cryptoService = cryptoService,
            credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT
        )

        Napier.d("Oid4vciService.createAuthRequest")
        val authRequest = oid4vciService.createAuthRequest()

        Napier.d("HTTP.GET (${metadata.authorizationEndpointUrl})")
        val codeUrl = client.get(metadata.authorizationEndpointUrl) {
            authRequest.encodeToParameters().forEach {
                this.parameter(it.key, it.value)
            }
            headers["X-Auth-Token"] = xAuthToken
        }.headers[HttpHeaders.Location]

        if (codeUrl == null) {
            throw Exception("codeUrl is null")
        }
        val code = Url(codeUrl).parameters["code"]
        if (code == null) {
            throw Exception("code is null")
        }

        val tokenRequest = oid4vciService.createTokenRequestParameters(code)
        Napier.d("Created tokenRequest")
        val tokenResponse: TokenResponseParameters = client.submitForm(metadata.tokenEndpointUrl.toString()) {
            setBody(tokenRequest.encodeToParameters().formUrlEncode())
        }.body()

        Napier.d("Received tokenResponse")
        val credentialRequest = oid4vciService.createCredentialRequest(tokenResponse, metadata)
        Napier.d("Created credentialRequest")
        val credentialResponse: CredentialResponseParameters =
            client.post(metadata.credentialEndpointUrl.toString()) {
                contentType(ContentType.Application.Json)
                setBody(credentialRequest)
                headers["Authorization"] = "$TOKEN_PREFIX_BEARER${tokenResponse.accessToken}"
            }.body()
        Napier.d("Received credentialResponse")

        credentialResponse.credential?.let {
            when (credentialResponse.format){
                CredentialFormatEnum.NONE -> TODO("Function not implemented")
                CredentialFormatEnum.JWT_VC ->
                    holderAgent.storeCredentials(listOf(Holder.StoreCredentialInput.Vc(vcJws = it, scheme = at.asitplus.wallet.idaustria.IdAustriaScheme, attachments = null)))
                CredentialFormatEnum.JWT_VC_SD ->
                    holderAgent.storeCredentials(listOf(Holder.StoreCredentialInput.SdJwt(vcSdJwt = it, scheme = at.asitplus.wallet.idaustria.IdAustriaScheme)))
                CredentialFormatEnum.JWT_VC_JSON_LD -> TODO("Function not implemented")
                CredentialFormatEnum.JSON_LD -> TODO("Function not implemented")
                CredentialFormatEnum.MSO_MDOC -> TODO("Function not implemented")
            }
        }
    }
}