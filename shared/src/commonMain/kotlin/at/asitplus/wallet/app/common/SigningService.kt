package at.asitplus.wallet.app.common

import CscAuthorizationDetails
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.rqes.CredentialInfo
import at.asitplus.rqes.CscCredentialListRequest
import at.asitplus.rqes.CscCredentialListResponse
import at.asitplus.rqes.SignatureRequestParameters
import at.asitplus.rqes.SignatureResponse
import at.asitplus.rqes.enums.CertificateOptions
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.io.ByteArrayBase64Serializer
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import at.asitplus.wallet.lib.rqes.RqesOpenId4VpHolder
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class SigningService(
    val platformAdapter: PlatformAdapter,
    val dataStoreService: DataStoreService,
    val errorService: ErrorService,
    httpService: HttpService,
) {
    var redirectUri: String? = null
    var signatureReguestParameter: SignatureRequestParameters? = null
    var document: ByteArray? = null
    var documentWithLabel: DocumentWithLabel? = null

    val pdfSigningService = "https://apps.egiz.gv.at/qtsp"

    var config = runBlocking { importFromDataStore() }

    var dtbsrAuthenticationDetails: AuthorizationDetails? = null
    var transactionTokens: List<String>? = null

    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage)
    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback/signing"

    lateinit var rqesWalletService: RqesOpenId4VpHolder

    suspend fun importFromDataStore(): SigningConfig {
        val serializedCredentialInfo = dataStoreService.getPreference("signingConfig").first()
        if (serializedCredentialInfo != null) {
            try {
                return vckJsonSerializer.decodeFromString<SigningConfig>(serializedCredentialInfo)
            } catch (e: Throwable) {
                return defaultSigningConfig
            }
        } else {
            return defaultSigningConfig
        }
    }

    suspend fun exportToDataStore() {
        dataStoreService.setPreference(key = "signingConfig", value = vckJsonSerializer.encodeToString(this.config))
    }

    fun init(redirectUrl: String = this.redirectUrl){
        rqesWalletService =
            RqesOpenId4VpHolder(redirectUrl = redirectUrl, clientId = config.getCurrent().oauth2ClientId)
    }

    suspend fun preloadCertificate() {
        init(this.redirectUrl + "/preload")

        val targetUrl = createServiceAuthRequest()
        redirectUri = this.redirectUrl + "/preload"
        platformAdapter.openUrl(targetUrl)
    }

    suspend fun resumePreloadCertificate(url: String) {
        val token = getTokenFromAuthCode(url)
        val credentialInfo = getCredentialInfo(token)
        config.getCurrent().credentialInfo = credentialInfo
        exportToDataStore()
    }

    suspend fun start(url: String) {
        init()

        extractSignatureRequestParameter(url)

        if (config.getCurrent().credentialInfo == null) {
            val targetUrl = createServiceAuthRequest()
            redirectUri = this.redirectUrl
            platformAdapter.openUrl(targetUrl)
        } else {
            rqesWalletService.setSigningCredential(config.getCurrent().credentialInfo!!)
            val targetUrl = createCredentialAuthRequest()
            redirectUri = this.redirectUrl
            platformAdapter.openUrl(targetUrl)
        }
    }

    suspend fun resumeWithServiceAuthCode(url: String) {
        val token = getTokenFromAuthCode(url)
        val credentialInfo = getCredentialInfo(token)

        rqesWalletService.setSigningCredential(credentialInfo)

        val targetUrl = createCredentialAuthRequest()
        redirectUri = this.redirectUrl
        platformAdapter.openUrl(targetUrl)
    }


    suspend fun resumeWithCredentialAuthCode(url: String) {
        val token = getTokenFromAuthCode(url)

        val signAlgorithm = rqesWalletService.signingCredential?.supportedSigningAlgorithms?.first() ?: X509SignatureAlgorithm.RS512

        val signHashRequest = rqesWalletService.createSignHashRequestParameters(
            dtbsr = (this.dtbsrAuthenticationDetails as CscAuthorizationDetails).documentDigests.map { it.hash },
            sad = token.accessToken,
            signatureAlgorithm = signAlgorithm
        )

        val signatures = client.post("${config.getCurrent().qtspBaseUrl}/signatures/signHash") {
            contentType(Json)
            accept(Json)
            setBody(vckJsonSerializer.encodeToString(signHashRequest))
        }.body<SignatureResponse>()

        val transactionTokens =
            this.transactionTokens ?: throw Throwable("Missing transactionTokens")
        val signedDocuments = getFinishedDocuments(client, pdfSigningService, signatures, transactionTokens)


        val signedDocList = vckJsonSerializer.encodeToJsonElement(
            ListSerializer(ByteArrayBase64Serializer),
            signedDocuments.map { it.document })

        val responseState =
            this.signatureReguestParameter?.state ?: throw Throwable("Missing responseState")
        val responseUrl =
            this.signatureReguestParameter?.responseUrl ?: throw Throwable("Missing responseUrl")
        val drivingAppResponseUrl = URLBuilder(responseUrl).apply {
            parameters.append("state", responseState)
        }.buildString()

        val finalRedirect = client.post(drivingAppResponseUrl) {
            contentType(FormUrlEncoded)
            setBody(
                JsonObject(mapOf("DocumentWithSignature" to signedDocList)).encodeToParameters()
                    .formUrlEncode()
            )
        }
    }

    suspend fun createServiceAuthRequest(): String{
        val authRequest =
            rqesWalletService.createServiceAuthenticationRequest()

        val targetUrl = URLBuilder("${config.getCurrent().oauth2BaseUrl}/oauth2/authorize").apply {
            authRequest.encodeToParameters().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()
        return targetUrl
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun extractSignatureRequestParameter(url: String) {
        val url = URLBuilder(url)
        val requestUri = url.parameters["request_uri"] ?: throw Throwable("Missing request_uri")
        val resp = client.get(requestUri)
        val jwt = resp.bodyAsText()
        val split = jwt.split(".")
        val payload = split[1]
        val payloadBytes = Base64.UrlSafe.withPadding(option = Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(payload)
        val payloadString = payloadBytes.decodeToString(0, 0 + payloadBytes.size)

        val signatureRequestParameter =
            vckJsonSerializer.decodeFromString<SignatureRequestParameters>(payloadString)
        this.signatureReguestParameter = signatureRequestParameter

        val documentLocation = signatureRequestParameter.documentLocations.first().uri
        val document = client.get(documentLocation).bodyAsBytes()
        val documentLabel = signatureRequestParameter.documentDigests.first().label
        this.document = document
        this.documentWithLabel = DocumentWithLabel(document, documentLabel)
    }

    suspend fun getTokenFromAuthCode(url: String): TokenResponseParameters {
        val tokenUrl = "${config.getCurrent().oauth2BaseUrl}/oauth2/token"

        val url = URLBuilder(url)
        val code = url.parameters["code"] ?: throw Throwable("Missing code")
        val state = url.parameters["state"] ?: throw Throwable("Missing state")

        Napier.e("Code: $code, State: $state")

        val tokenRequest = rqesWalletService.createOAuth2TokenRequest(
            state,
            authorization = OAuth2Client.AuthorizationForToken.Code(code),
            authorizationDetails = setOf()
        )

        val tokenResponse = client.post(tokenUrl) {
            contentType(FormUrlEncoded)
            accept(Json)
            setBody(
                tokenRequest.encodeToParameters().formUrlEncode()
            )
        }.body<String>().also {
            Napier.d { "TokenResponseParameter $it" }
        }

        /**
         * Cannot parse to [TokenResponseParameters] bc authorizationDetails are set but empty
         * also currently missing `expires_in`
         */
        val tokenParsed = vckJsonSerializer.decodeFromString<JsonElement>(tokenResponse)

        val tokenParsedMap = (tokenParsed as? JsonObject)?.mapValues {
            if (it.key != "authorization_details") it.value
            else JsonPrimitive(null)
        }?.toMutableMap() ?: mutableMapOf()
        tokenParsedMap["expires_in"] = JsonPrimitive(3600)
        val tokenParsed2 = vckJsonSerializer.encodeToJsonElement(tokenParsedMap)
        val tokenResponseParameters =
            vckJsonSerializer.decodeFromJsonElement<TokenResponseParameters>(tokenParsed2)
        Napier.d { "$tokenResponse and $tokenParsed and $tokenResponseParameters" }

        return tokenResponseParameters
    }

    suspend fun getCredentialInfo(token: TokenResponseParameters): CredentialInfo {
        val credentialListRequest = CscCredentialListRequest(
            credentialInfo = true,
            certificates = CertificateOptions.SINGLE,
            certInfo = true,
            authInfo = true,
            onlyValid = true,
        )

        val credentialResponse = client.post("${config.getCurrent().qtspBaseUrl}/credentials/list") {
            accept(Json)
            contentType(Json)
            header(
                HttpHeaders.Authorization,
                "${token.tokenType} ${token.accessToken}"
            )
            setBody(vckJsonSerializer.encodeToString(credentialListRequest))
        }
        val credentialListResponse = credentialResponse.body<CscCredentialListResponse>()

        val credentialInfo = credentialListResponse.credentialInfos?.first()
            ?: throw Throwable("Missing credentialInfos")

        return credentialInfo
    }

    suspend fun createCredentialAuthRequest(): String{
        val signAlgorithm = rqesWalletService.signingCredential?.supportedSigningAlgorithms?.first() ?: X509SignatureAlgorithm.RS512

        val documentWithLabel =
            this.documentWithLabel ?: throw Throwable("Missing documentWithLabel")
        val dtbsr = listOf(getDTBSR(client = client, qtspHost = pdfSigningService, signatureAlgorithm = signAlgorithm, signingCredential = rqesWalletService.signingCredential!!, document = documentWithLabel))
        val transactionTokens = dtbsr.map { it.first }
        this.transactionTokens = transactionTokens

        val dtbsrAuthenticationDetails =
            rqesWalletService.getCscAuthenticationDetails(dtbsr.map { it.second }, hashAlgorithm = signAlgorithm.digest)
        this.dtbsrAuthenticationDetails = dtbsrAuthenticationDetails

        val authRequest = rqesWalletService.
        createCredentialAuthenticationRequest(
            documentDigests = dtbsr.map { it.second },
            redirectUrl = "${this.redirectUrl}/finalize",
            hashAlgorithm = signAlgorithm.digest,
        )

        val targetUrl = URLBuilder("${config.getCurrent().oauth2BaseUrl}/oauth2/authorize").apply {
            authRequest.encodeToParameters().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()

        return targetUrl
    }
}

@Serializable
data class SigningConfig(
    val qtsps: List<QtspConfig>,
    var current: String
) {
    fun getCurrent(): QtspConfig{
        return this.qtsps.first { it.identifier == this.current }
    }

    fun getQtspByIdentifier(identifier: String): QtspConfig {
        return this.qtsps.first { it.identifier == identifier }
    }
}

val defaultSigningConfig = SigningConfig(qtsps = listOf(
    QtspConfig("EGIZ", "https://apps.egiz.gv.at/qtsp/csc/v2", "https://apps.egiz.gv.at/qtsp", "https://wallet.a-sit.at/app"),
    QtspConfig("ATRUST", "https://hs-abnahme.a-trust.at/csc/v2", "https://hs-abnahme.a-trust.at/csc/v1", "WALLET_EGIZ")),
    current = "EGIZ")

@Serializable
data class QtspConfig(
    val identifier: String,
    val qtspBaseUrl: String,
    val oauth2BaseUrl: String,
    val oauth2ClientId: String,
    var credentialInfo: CredentialInfo? = null,
)


