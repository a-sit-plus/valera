package at.asitplus.wallet.app.common

import CscAuthorizationDetails
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.TokenResponseParameters
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
import at.asitplus.wallet.lib.rqes.RqesWalletService
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
    private val config: WalletConfig,
) {
    var redirectUri: String? = null
    var signatureReguestParameter: SignatureRequestParameters? = null
    var document: ByteArray? = null
    var documentWithLabel: DocumentWithLabel? = null

    lateinit var qtspHost: String

    var dtbsrAuthenticationDetails: AuthorizationDetails? = null
    var transactionTokens: List<String>? = null

    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage)
    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback/signing"

    val rqesWalletService = RqesWalletService(redirectUrl = redirectUrl)

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun sign(url: String) {
        qtspHost = runBlocking { config.qtspHost.first() }

        val url = URLBuilder(url)

        val requestUri = url.parameters["request_uri"]
        val resp = client.get(requestUri!!)
        val jwt = resp.bodyAsText()
        val split = jwt.split(".")
        val payload = split[1]
        val payloadBytes = Base64.UrlSafe.withPadding(option = Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload)
        val payloadString = payloadBytes.decodeToString(0, 0 + payloadBytes.size)

        val signatureRequestParameter = vckJsonSerializer.decodeFromString<SignatureRequestParameters>(payloadString)
        this.signatureReguestParameter = signatureRequestParameter

        val documentLocation = signatureRequestParameter.documentLocations.first().uri
        val document = client.get(documentLocation).bodyAsBytes()
        val documentLabel = signatureRequestParameter.documentDigests.first().label
        this.document = document
        this.documentWithLabel = DocumentWithLabel(document, documentLabel)

        val authRequest = rqesWalletService.createOAuth2AuthenticationRequest(scope = RqesWalletService.RqesOauthScope.SERVICE)

        val targetUrl = URLBuilder("${qtspHost}/oauth2/authorize").apply {
            authRequest.encodeToParameters().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()
        redirectUri = this.redirectUrl
        platformAdapter.openUrl(targetUrl)
    }

    suspend fun resumeWithAuthCode(url: String) {
        val tokenUrl = "${qtspHost}/oauth2/token"

        val url = URLBuilder(url)
        val code = url.parameters["code"]!!
        val state = url.parameters["state"]!!

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


        //TODO check wieso authInfo in [CredentialListRequest] nullable aber nicht in [CredentialListResponse]
        val credentialListRequest = CscCredentialListRequest(
            credentialInfo = true,
            certificates = CertificateOptions.SINGLE,
            certInfo = true,
            authInfo = true,
            onlyValid = true,
        )

        val credentialResponse = client.post("${qtspHost}/csc/v2/credentials/list") {
                accept(Json)
                contentType(Json)
                header(
                    HttpHeaders.Authorization,
                    "${tokenResponseParameters.tokenType} ${tokenResponseParameters.accessToken}"
                )
                setBody(vckJsonSerializer.encodeToString(credentialListRequest))
            }
        val credentialListResponse = credentialResponse.body<CscCredentialListResponse>()

        val credentialInfo = credentialListResponse.credentialInfos?.first()!!

        rqesWalletService.setSigningCredential(credentialInfo)

        /**
         * TODO remove
         * EGIZ qtsp only supports RS512 for now
         */
        rqesWalletService.updateCryptoProperties(
            signAlgorithm = if (!qtspHost.contains("egiz")) rqesWalletService.signingCredential!!.supportedSigningAlgorithms.first() else X509SignatureAlgorithm.RS512
        )

        val dtbsr = listOf(getDTBSR(client, qtspHost, rqesWalletService, this.documentWithLabel!!))
        val transactionTokens = dtbsr.map { it.first }
        this.transactionTokens = transactionTokens

        val dtbsrAuthenticationDetails = rqesWalletService.getCscAuthenticationDetails(dtbsr.map { it.second })
        this.dtbsrAuthenticationDetails = dtbsrAuthenticationDetails

        val authRequest = rqesWalletService.createOAuth2AuthenticationRequest(
            scope = RqesWalletService.RqesOauthScope.CREDENTIAL,
            redirectUrl = "${this.redirectUrl}/finalize",
            authorizationDetails = listOf(dtbsrAuthenticationDetails),
        )

        /**
         * Using browser in case TOS needs to be accepted etc (current default and only way)
         */
        val targetUrl = URLBuilder("${qtspHost}/oauth2/authorize").apply {
            authRequest.encodeToParameters().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()
        redirectUri = this.redirectUrl
        platformAdapter.openUrl(targetUrl)
    }

    suspend fun finalizeWithAuthCode(url: String) {
        val tokenUrl = "${qtspHost}/oauth2/token"

        val url = URLBuilder(url)
        val code = url.parameters["code"]!!
        val state = url.parameters["state"]!!

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

        val signHashRequest = rqesWalletService.createSignHashRequestParameters(
            dtbsr = (this.dtbsrAuthenticationDetails as CscAuthorizationDetails).documentDigests.map { it.hash },
            sad = tokenResponseParameters.accessToken,
        )

        val signatures = client.post("${qtspHost}/csc/v2/signatures/signHash") {
            contentType(Json)
            accept(Json)
            setBody(vckJsonSerializer.encodeToString(signHashRequest))
        }.body<SignatureResponse>()

        print(signatures)
        val signedDocuments = getFinishedDocuments(client, qtspHost, signatures, transactionTokens!!)


        val signedDocList = vckJsonSerializer.encodeToJsonElement(
            ListSerializer(ByteArrayBase64Serializer),
            signedDocuments.map { it.document })

        val responseState = this.signatureReguestParameter?.state
        val responseUrl = this.signatureReguestParameter?.responseUrl
        val drivingAppResponseUrl = URLBuilder(responseUrl!!).apply {
            parameters.append("state", responseState!!)
        }.buildString()

        val finalRedirect = client.post(drivingAppResponseUrl) {
            contentType(FormUrlEncoded)
            setBody(
                JsonObject(mapOf("DocumentWithSignature" to signedDocList)).encodeToParameters().formUrlEncode()
            )
        }
    }
}