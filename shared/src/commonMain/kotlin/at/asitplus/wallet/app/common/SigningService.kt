package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.rqes.CredentialInfo
import at.asitplus.rqes.CredentialInfoRequest
import at.asitplus.rqes.CredentialListRequest
import at.asitplus.rqes.CredentialListResponse
import at.asitplus.rqes.CscAuthorizationDetails
import at.asitplus.rqes.QtspSignatureResponse
import at.asitplus.rqes.SignatureRequestParameters
import at.asitplus.rqes.enums.CertificateOptions
import at.asitplus.signum.indispensable.Digest
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.io.ByteArrayBase64Serializer
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_sign_successful
import at.asitplus.wallet.app.common.Configuration.DATASTORE_SIGNING_CONFIG
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.compose.resources.getString
import ui.navigation.IntentService
import kotlin.time.Clock

class SigningService(
    val intentService: IntentService,
    val dataStoreService: DataStoreService,
    val errorService: ErrorService,
    val snackbarService: SnackbarService,
    httpService: HttpService,
) {
    val config = runBlocking { importFromDataStore() }
    var state: SigningState? = null

    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage)
    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback/signing"
    private val pdfSigningService = "https://apps.egiz.gv.at/qtsp"
    lateinit var rqesWalletService: RqesOpenId4VpHolder

    private lateinit var signatureRequestParameter: SignatureRequestParameters
    private lateinit var documentWithLabel: MutableMap<Int, DocumentWithLabel>
    private lateinit var dtbsrAuthenticationDetails: AuthorizationDetails
    private lateinit var transactionTokens: List<String>
    private var serviceToken: TokenResponseParameters? = null

    private val pdfSigningAlgorithms = listOf(
        "1.2.840.113549.1.1.11", //RSA_SHA256
        "1.2.840.113549.1.1.12", //RSA_SHA384
        "1.2.840.113549.1.1.13", //RSA_SHA512
        "2.16.840.1.101.3.4.3.14", //RSA_SHA3_256
        "2.16.840.1.101.3.4.3.15", //RSA_SHA3_384
        "2.16.840.1.101.3.4.3.16", //RSA_SHA3_512
        "1.2.840.10045.4.3.2", //ECDSA_SHA256
        "1.2.840.10045.4.3.3", //ECDSA_SHA384
        "1.2.840.10045.4.3.4", //ECDSA_SHA512
        "2.16.840.1.101.3.4.3.10", //ECDSA_SHA3_256
        "2.16.840.1.101.3.4.3.11", //ECDSA_SHA3_384
        "2.16.840.1.101.3.4.3.12", //ECDSA_SHA3_512
    )

    suspend fun reset() {
        dataStoreService.deletePreference(DATASTORE_SIGNING_CONFIG)
    }

    private suspend fun importFromDataStore(): SigningConfig =
        catchingUnwrapped {
            vckJsonSerializer.decodeFromString<SigningConfig>(
                dataStoreService.getPreference(
                    DATASTORE_SIGNING_CONFIG
                ).first()!!
            )
        }
            .getOrElse { defaultSigningConfig }

    suspend fun exportToDataStore() {
        dataStoreService.setPreference(
            key = DATASTORE_SIGNING_CONFIG,
            value = vckJsonSerializer.encodeToString(this.config)
        )
    }

    suspend fun setCurrentQtsp(qtsp: String) {
        config.current = qtsp
        exportToDataStore()
    }

    suspend fun preloadCertificate() {
        rqesWalletService =
            RqesOpenId4VpHolder(redirectUrl = redirectUrl, clientId = config.getCurrent().oauth2ClientId)

        val targetUrl = createServiceAuthRequest()
        this.state = SigningState.PreloadCredential
        intentService.openIntent(
            url = targetUrl,
            redirectUri = this.redirectUrl,
            intentType = IntentService.IntentType.SigningPreloadIntent
        )
    }

    suspend fun resumePreloadCertificate(url: String) {
        val token = getTokenFromAuthCode(url)
        val credentialInfo = getCredentialInfo(token)
        config.getCurrent().credentialInfo = credentialInfo
        exportToDataStore()
    }

    suspend fun start(signatureRequestParameters: SignatureRequestParameters) {
        rqesWalletService =
            RqesOpenId4VpHolder(redirectUrl = redirectUrl, clientId = config.getCurrent().oauth2ClientId)
        this.signatureRequestParameter = signatureRequestParameters
        prepareDocuments()

        if (config.hasValidCertificate()) {
            val credentialInfo = config.getCurrent().credentialInfo ?: throw Throwable("Missing credentialInfo")
            rqesWalletService.setSigningCredential(credentialInfo)
            val targetUrl = createCredentialAuthRequest()
            this.state = SigningState.CredentialRequest
            intentService.openIntent(
                url = targetUrl,
                redirectUri = this.redirectUrl,
                intentType = IntentService.IntentType.SigningCredentialIntent
            )
        } else {
            val targetUrl = createServiceAuthRequest()
            this.state = SigningState.ServiceRequest
            intentService.openIntent(
                url = targetUrl,
                redirectUri = this.redirectUrl,
                intentType = IntentService.IntentType.SigningServiceIntent
            )
        }
    }

    suspend fun resumeWithServiceAuthCode(url: String) {
        getTokenFromAuthCode(url).let { serviceToken ->
            this.serviceToken = serviceToken
            val credentialInfo = getCredentialInfo(serviceToken)
            if (config.getCurrent().allowPreload) {
                config.getCurrent().credentialInfo = credentialInfo
                exportToDataStore()
            }
            rqesWalletService.setSigningCredential(credentialInfo)

            val targetUrl = createCredentialAuthRequest()

            intentService.openIntent(
                url = targetUrl,
                redirectUri = this.redirectUrl,
                intentType = IntentService.IntentType.SigningCredentialIntent
            )
        }
    }

    suspend fun resumeWithCredentialAuthCode(url: String) {
        val credentialToken = getTokenFromAuthCode(url)

        val signAlgorithm =
            rqesWalletService.signingCredential?.supportedSigningAlgorithms?.first() ?: X509SignatureAlgorithm.RS512

        val signHashRequest = rqesWalletService.createSignHashRequestParameters(
            dtbsr = (this.dtbsrAuthenticationDetails as CscAuthorizationDetails).documentDigests.map { it.hash },
            sad = credentialToken.accessToken,
            signatureAlgorithm = signAlgorithm
        )
        val token = serviceToken ?: credentialToken
        serviceToken = null

        val signatures = client.post("${config.getCurrent().qtspBaseUrl}/signatures/signHash") {
            contentType(Json)
            accept(Json)
            header(
                HttpHeaders.Authorization,
                "${token.tokenType} ${token.accessToken}"
            )
            setBody(vckJsonSerializer.encodeToString(signHashRequest))
        }.body<QtspSignatureResponse>()

        val transactionTokens = this.transactionTokens
        val signedDocuments = getFinishedDocuments(
            client,
            pdfSigningService,
            signatures,
            transactionTokens,
            config.getCurrent().identifier
        )


        val signedDocList = vckJsonSerializer.encodeToJsonElement(
            ListSerializer(ByteArrayBase64Serializer),
            signedDocuments.map { it.document })

        val responseState = this.signatureRequestParameter.state
        val responseUrl =
            this.signatureRequestParameter.responseUrl ?: throw Throwable("Missing responseUrl")

        val drivingAppResponseUrl = URLBuilder(responseUrl).apply {
            if (responseState != null) {
                parameters.append("state", responseState)
            }
        }.buildString()

        val response = client.post(drivingAppResponseUrl) {
            contentType(FormUrlEncoded)
            setBody(
                JsonObject(mapOf("documentWithSignature" to signedDocList)).encodeToParameters()
                    .formUrlEncode()
            )
        }
        catchingUnwrapped { response.body<QtspFinalRedirect>() }.getOrNull()?.let {
            intentService.openIntent(it.redirect_uri)
        }
        snackbarService.showSnackbar(getString(Res.string.snackbar_sign_successful))
    }

    private suspend fun createServiceAuthRequest(): String {
        val authRequest =
            rqesWalletService.createServiceAuthenticationRequest()

        return URLBuilder("${config.getCurrent().oauth2BaseUrl}/oauth2/authorize").apply {
            authRequest.encodeToParameters<AuthenticationRequestParameters>().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()
    }

    suspend fun prepareDocuments() {
        this.documentWithLabel = mutableMapOf()
        this.signatureRequestParameter.documentLocations.forEachIndexed { index, documentLocation ->
            client.get(documentLocation.uri).bodyAsBytes().let {
                this.documentWithLabel[index] = DocumentWithLabel(
                    document = it,
                    label = this.signatureRequestParameter.documentDigests[index].label
                )
            }
        }
    }

    suspend fun parseSignatureRequestParameter(url: String): SignatureRequestParameters {
        val requestUri = URLBuilder(url).parameters["request_uri"] ?: throw Throwable("Missing request_uri")
        val resp = client.get(requestUri)
        val jwt = resp.bodyAsText()

        return JwsSigned.deserialize(
            SignatureRequestParameters.serializer(),
            jwt,
            vckJsonSerializer
        ).getOrElse {
            throw Throwable("SigningService: Unable to parse SignatureRequestParameters", it)
        }.payload
    }

    suspend fun getTokenFromAuthCode(url: String): TokenResponseParameters {
        val tokenUrl = "${config.getCurrent().oauth2BaseUrl}/oauth2/token"

        val code = URLBuilder(url).parameters["code"] ?: throw Throwable("Missing code")
        val state = URLBuilder(url).parameters["state"] ?: throw Throwable("Missing state")

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

    private suspend fun getCredentialInfo(token: TokenResponseParameters): CredentialInfo {
        val credentialListRequest = CredentialListRequest(
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
        val credentialListResponse = credentialResponse.body<CredentialListResponse>()

        if (credentialListResponse.credentialInfos.isNullOrEmpty()) {
            return getSingleCredentialInfo(token, credentialListResponse.credentialIDs.first())

        } else {
            return credentialListResponse.credentialInfos?.first()
                ?: throw Throwable("Missing credentialInfos")
        }
    }

    private suspend fun getSingleCredentialInfo(token: TokenResponseParameters, credentialId: String): CredentialInfo {
        val credentialInfoRequest = CredentialInfoRequest(
            credentialID = credentialId,
            certificates = CertificateOptions.SINGLE,
            certInfo = true,
            authInfo = true,
        )

        val credentialResponse = client.post("${config.getCurrent().qtspBaseUrl}/credentials/info") {
            accept(Json)
            contentType(Json)
            header(
                HttpHeaders.Authorization,
                "${token.tokenType} ${token.accessToken}"
            )
            setBody(vckJsonSerializer.encodeToString(credentialInfoRequest))
        }
        val credInfo = credentialResponse.body<CredentialInfo>()
        return CredentialInfo(
            credentialId, credInfo.description, credInfo.signatureQualifier, credInfo.keyParameters,
            credInfo.certParameters, credInfo.authParameters, credInfo.scal, credInfo.multisign
        )

    }

    private suspend fun createCredentialAuthRequest(): String {
        val signingCredential = rqesWalletService.signingCredential ?: throw Throwable("Missing signingCredential")

        val credentialSigningAlgorithms = signingCredential.supportedSigningAlgorithms
        val commonSigningAlgorithm =
            credentialSigningAlgorithms.filter { this.pdfSigningAlgorithms.contains(it.oid.toString()) }
        val signatureAlgorithm = catchingUnwrapped { commonSigningAlgorithm.first() }.getOrNull()
            ?: throw Throwable("Unsupported pdf signing algorithm")

        val dtbsr = this.documentWithLabel.map {
            getDTBSR(
                client = client,
                qtspHost = pdfSigningService,
                signatureAlgorithm = signatureAlgorithm,
                signingCredential = signingCredential,
                document = it.value
            )
        }
        this.transactionTokens = dtbsr.map { it.first }

        this.dtbsrAuthenticationDetails =
            rqesWalletService.getCscAuthenticationDetails(
                dtbsr.map { it.second },
                hashAlgorithm = signatureAlgorithm.digest,
                this.signatureRequestParameter.documentLocations
            )

        val authRequest = rqesWalletService.createCredentialAuthenticationRequest(
            documentDigests = dtbsr.map { it.second },
            hashAlgorithm = signatureAlgorithm.digest,
            documentLocation = this.signatureRequestParameter.documentLocations
        )

        return URLBuilder("${config.getCurrent().oauth2BaseUrl}/oauth2/authorize").apply {
            authRequest.encodeToParameters<AuthenticationRequestParameters>().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()
    }
}

enum class SigningState {
    ServiceRequest,
    CredentialRequest,
    PreloadCredential
}

@Serializable
data class SigningConfig(
    val qtsps: List<QtspConfig>,
    var current: String
) {
    fun getCurrent(): QtspConfig = this.qtsps.first { it.identifier == this.current }

    fun hasValidCertificate(): Boolean {
        getCurrent().credentialInfo?.certParameters?.validTo?.let {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            return LocalDateTime.parse(it, format = qesDateTime).compareTo(now) > 0
        }
        return false
    }

    fun getQtspByIdentifier(identifier: String): QtspConfig = this.qtsps.first { it.identifier == identifier }
}

val defaultSigningConfig = SigningConfig(
    qtsps = listOf(
        QtspConfig(
            "EGIZ",
            "https://apps.egiz.gv.at/qtsp/csc/v2",
            "https://apps.egiz.gv.at/qtsp",
            "https://wallet.a-sit.at/app",
            allowPreload = true
        ),
        QtspConfig(
            "ATRUST",
            "https://hs-abnahme.a-trust.at/csc/v2",
            "https://hs-abnahme.a-trust.at/csc/v1",
            "WALLET_EGIZ",
            allowPreload = true
        ),
        QtspConfig(
            "PRIMESIGN",
            "https://qs.primesign-test.com/csc/v2",
            "https://id.primesign-test.com/realms/qs-staging",
            "https://wallet.a-sit.at/app",
            allowPreload = false
        ),
        QtspConfig(
            "NAMIRIAL",
            "https://csc-api.fba.users.bit4id.click/api/csc/v2",
            "https://csc-api.fba.users.bit4id.click/api/csc/v2",
            "POTENTIAL-UC5",
            allowPreload = false
        )
    ),
    current = "EGIZ"
)

@Serializable
data class QtspConfig(
    val identifier: String,
    val qtspBaseUrl: String,
    val oauth2BaseUrl: String,
    val oauth2ClientId: String,
    var credentialInfo: CredentialInfo? = null,
    val allowPreload: Boolean
)

val qesDateTime = LocalDateTime.Format {
    year()
    monthNumber()
    day()
    hour()
    minute()
    second()
    char('Z')
}

@Serializable
data class QtspFinalRedirect(
    val redirect_uri: String
)


val X509SignatureAlgorithm.digest: Digest
    get() = when (this) {
        is X509SignatureAlgorithm.ECDSA -> digest
        is X509SignatureAlgorithm.RSAPSS -> digest
        is X509SignatureAlgorithm.RSAPKCS1 -> digest
    }