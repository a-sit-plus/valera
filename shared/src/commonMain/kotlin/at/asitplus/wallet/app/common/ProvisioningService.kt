package at.asitplus.wallet.app.common

import DataStoreService
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

const val HOST = "https://wallet.a-sit.at"
const val PATH_WELL_KNOWN_CREDENTIAL_ISSUER = "/.well-known/openid-credential-issuer"

class ProvisioningService(val platformAdapter: PlatformAdapter, val dataStoreService: DataStoreService, val cryptoService: CryptoService, val holderAgent: HolderAgent) {
    private  val cookieStorage = PersistentCookieStorage(dataStoreService)
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
    suspend fun step1(){ // TODO: Give meaningful method name
        Napier.d("ProvisioningService: Start provisioning")
        val response = client.get("$HOST/m1/oauth2/authorization/idaq")
        val urlToOpen = response.headers["Location"]

        val xAuthToken = response.headers["X-Auth-Token"]
        if (xAuthToken == null){
            //throw Exception("X-Auth-Token not received")
        }

        println("ProvisioningService: [step1] Store X-Auth-Token: $xAuthToken")
        dataStoreService.setData(xAuthToken ?: "", Resources.DATASTORE_KEY_XAUTH)

        if (urlToOpen != null) {
            Napier.d("ProvisioningService: [step2] Open URL: $urlToOpen")
            platformAdapter.openUrl(urlToOpen)
        } else {
            throw Exception("Redirect not found in header")
        }
    }
    suspend fun step3(url: String){ // TODO: Give meaningful method name
        val xAuthToken = dataStoreService.getData(Resources.DATASTORE_KEY_XAUTH)
        if (xAuthToken == null){
            //throw Exception("X-Auth-Token not available in DataStoreService")
        }
        Napier.d("ProvisioningService: [step3] Create request with x-auth: $xAuthToken")
        client.get(url) {
            headers["X-Auth-Token"] = xAuthToken ?: ""
        }

        step4()
    }

    suspend fun step4(){ // TODO: Give meaningful method name
        val xAuthToken = dataStoreService.getData(Resources.DATASTORE_KEY_XAUTH)
        if (xAuthToken == null){
            //throw Exception("X-Auth-Token not available in DataStoreService")
        }
        Napier.d("ProvisioningService: [step4] Load X-Auth-Token: $xAuthToken")
        val metadata: IssuerMetadata = client.get("$HOST/m1$PATH_WELL_KNOWN_CREDENTIAL_ISSUER"){
            headers["X-Auth-Token"] = xAuthToken ?: ""
        }.body()

        val oid4vciService = WalletService(
            credentialScheme = at.asitplus.wallet.idaustria.ConstantIndex.IdAustriaCredential,
            clientId = "$HOST/m1",
            cryptoService = cryptoService,
            credentialRepresentation = ConstantIndex.CredentialRepresentation.SD_JWT
        )

        Napier.d("ProvisioningService: [step4] Oid4vciService.createAuthRequest")
        val authRequest = oid4vciService.createAuthRequest()

        Napier.d("ProvisioningService: [step4] HTTP.GET (${metadata.authorizationEndpointUrl})")
        val codeUrl = client.get(metadata.authorizationEndpointUrl) {
            authRequest.encodeToParameters().forEach {
                println("authRequest.encodeToParameters(): $it")
                this.parameter(it.key, it.value)
            }
            headers["X-Auth-Token"] = xAuthToken ?: ""
        }.headers[HttpHeaders.Location]

        if (codeUrl == null) {
            throw Exception("codeUrl is null")
        }
        val code = Url(codeUrl).parameters["code"]
        if (code == null) {
           throw Exception("code is null")
        }

        val tokenRequest = oid4vciService.createTokenRequestParameters(code)
        Napier.d("ProvisioningService: [step4] Created tokenRequest")
        val tokenResponse: TokenResponseParameters = client.submitForm(metadata.tokenEndpointUrl!!) {
            setBody(tokenRequest.encodeToParameters().formUrlEncode())
        }.body()

        Napier.d("ProvisioningService: [step4] Received tokenResponse")
        val credentialRequest = oid4vciService.createCredentialRequest(tokenResponse, metadata)
        Napier.d("ProvisioningService: [step4] Created credentialRequest")
        val credentialResponse: CredentialResponseParameters =
            client.post(metadata.credentialEndpointUrl!!) {
                contentType(ContentType.Application.Json)
                setBody(credentialRequest)
                headers["Authorization"] = "$TOKEN_PREFIX_BEARER${tokenResponse.accessToken}"
            }.body()
        Napier.d("ProvisioningService: [step4] Received credentialResponse")

        credentialResponse.credential?.let {
            when (credentialResponse.format){
                CredentialFormatEnum.NONE -> TODO()
                CredentialFormatEnum.JWT_VC ->
                    holderAgent.storeCredentials(listOf(Holder.StoreCredentialInput.Vc(vcJws = it, scheme = at.asitplus.wallet.idaustria.ConstantIndex.IdAustriaCredential, attachments = null)))
                CredentialFormatEnum.JWT_VC_SD ->
                    holderAgent.storeCredentials(listOf(Holder.StoreCredentialInput.SdJwt(vcSdJwt = it, scheme = at.asitplus.wallet.idaustria.ConstantIndex.IdAustriaCredential)))
                CredentialFormatEnum.JWT_VC_JSON_LD -> TODO()
                CredentialFormatEnum.JSON_LD -> TODO()
                CredentialFormatEnum.MSO_MDOC -> TODO()
            }



        }
    }


}