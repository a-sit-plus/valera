package at.asitplus.wallet.app.common

import at.asitplus.dif.rqes.CollectionEntries.DocumentDigestEntries.OAuthDocumentDigest
import at.asitplus.dif.rqes.CollectionEntries.DocumentLocation
import at.asitplus.dif.rqes.Method
import at.asitplus.dif.rqes.SignDocParameters
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.openid.rqes.RqesRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidvci.RqesWalletService
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import com.benasher44.uuid.uuid4
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.util.collections.ConcurrentMap
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

private data class RqesEntry(
    val rqesRequest: RqesRequest,
    val authorizationDetails: AuthorizationDetails,
)

class SigningService(
    val platformAdapter: PlatformAdapter,
    httpService: HttpService
) {
    private val client = httpService.buildHttpClient()

    //    private val target = "https://apps.egiz.gv.at/qtsp"
    private val target = "http://127.0.0.1:7086/qtsp"
    private val clientId = "https://wallet.a-sit.at/mobile"

    private val rqesWalletService = RqesWalletService(
        clientId = clientId,
        redirectUrl = clientId
    )

    private val rqesMutex = ConcurrentMap<String, RqesEntry>()

    suspend fun sign() {
        println("SigningService: sign()")

        val testRequest = RqesRequest(
            responseType = "TODO",
            clientId = "TODO",
            responseMode = OpenIdConstants.ResponseMode.DIRECT_POST,
            responseUri = "Must be present if direct post",
            nonce = uuid4().toString(),
            documentDigests = listOf(
                OAuthDocumentDigest(
                    hash = "abc".decodeBase64Bytes(), label = "efg"
                )
            ),
            documentLocations = listOf(
                DocumentLocation(
                    uri = Url("https://www.google.com"), method = Method.Public
                )
            ),
            clientData = null
        )
        sign(testRequest)
    }

    suspend fun sign(request: RqesRequest) {

        val authRequest = rqesWalletService.createOAuth2AuthRequest(
            request
        )
        rqesMutex[authRequest.state ?: throw Exception("no state")] = RqesEntry(request, authRequest.authorizationDetails!!.first())

        /**
         * Using browser in case TOS needs to be accepted etc (current default and only way)
         */
        val targetUrl = URLBuilder("$target/oauth2/authorize").apply {
            authRequest.encodeToParameters().forEach {
                parameters.append(it.key, it.value)
            }
        }.buildString()

        platformAdapter.openUrl(targetUrl)
        /**
         * Alternative access using app short cut - not opening browser
         */
//        val presentationRequest = kotlin.runCatching {
//            client.get("$target/oauth2/authorize") {
//                authRequest.encodeToParameters().forEach {
//                    this.parameter(it.key, it.value)
//                }
//            }
//        }.getOrElse { throw Exception("Did not receive presentation request from Q") }
//        Napier.w("We received a request poggers $presentationRequest")
//        platformAdapter.openUrl(
//            presentationRequest.headers[HttpHeaders.Location] ?: throw Exception("codeUrl is null")
//        )
    }

    //TODO rename function
    suspend fun oauth2TokenAfterSuccessfulPresentationCSC(location: String) {
        val test = Url(location)
        val state = test.parameters["state"] ?: throw Exception("No state in URL")
        val tokenRequest = rqesWalletService.createOauth2TokenRequestParameters(
            state = state,
            authorizationDetails = rqesMutex[state]?.authorizationDetails?.let { setOf(it) } ?: throw Exception("Missing authorizationDetails"),
            authorization = WalletService.AuthorizationForToken.Code(
                test.parameters["code"] ?: throw Exception("Missing authorization")
            )
        )

        val test131 = tokenRequest.encodeToParameters().formUrlEncode()
        val tokenResponse = client.post("$target/oauth2/token") {
            contentType(FormUrlEncoded)
            accept(Json)
            setBody(
                test131
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

        /**
         * QTSP requires SAD, which may or may not be correct?
         */
        val testSignDoc = (rqesWalletService.createSignDocRequestParameters(
            rqesMutex[state]?.rqesRequest ?: throw Exception("Missing original Request")
        ) as SignDocParameters).copy(
            sad = tokenResponseParameters.accessToken
        )
        val testSign = client.post("$target/csc/v2/signatures/signDoc") {
            contentType(Json)
            accept(Json)
            setBody(testSignDoc)
        }

        /**
         * TODO echte data class in VCK
         */
        val body = testSign.body<String>()
        val code = testSign.status
        Napier.d { "$code and $body" }
        /**
         * TODO respond to driving application
         */
    }

    suspend fun sign(redirectUrl: String, clientId: String) {
        val url = Url(redirectUrl)
        val test = client.get(url)

        /**
         * TODO: Cleaner...
         * We receive a JWT with the body being a base64 encoded [RqesRequest]
         * Also currently hashOID missing
         */
        val body = test.body<String>().split(".")
        val bodyDecoded = body[1].decodeBase64String()
        val rqesRequest = vckJsonSerializer.decodeFromString<RqesRequest>(bodyDecoded)


        Napier.d { "${test.status} with $rqesRequest" }
        sign(rqesRequest)

    }

}

//private fun RqesRequest.toOAuth2AuthenticationRequest(): AuthenticationRequestParameters {
//    val cscCredential = AuthorizationDetails.CSCCredential(
//        documentDigests = this.documentDigests,
//        documentLocations = this.documentLocations,
//        hashAlgorithmOID = this.hashAlgorithmOid,
//    )
//
//    val test = AuthenticationRequestParameters(
//
//    )
//
//}
