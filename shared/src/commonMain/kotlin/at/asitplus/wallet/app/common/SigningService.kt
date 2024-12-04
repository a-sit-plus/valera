package at.asitplus.wallet.app.common

import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.rqes.Method
import at.asitplus.rqes.SignatureRequestParameters
import at.asitplus.rqes.collection_entries.DocumentLocation
import at.asitplus.rqes.collection_entries.OAuthDocumentDigest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import at.asitplus.wallet.lib.rqes.RqesWalletService
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
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.collections.set

private data class RqesEntry(
    val clientId: String,
    val rqesRequest: SignatureRequestParameters,
    val authorizationDetails: AuthorizationDetails,
)

class SigningService(
    val platformAdapter: PlatformAdapter,
    httpService: HttpService
) {
    private val client = httpService.buildHttpClient()

    private val target = "https://apps.egiz.gv.at/qtsp"
    //    private val target = "http://127.0.0.1:7086/qtsp"
    private val clientId = "https://wallet.a-sit.at/mobile"

    private val rqesWalletService = RqesWalletService(
        clientId = clientId,
        redirectUrl = clientId
    )

    private val rqesMutex = ConcurrentMap<String, RqesEntry>()

    suspend fun sign() {
        println("SigningService: sign()")

        val testRequest = SignatureRequestParameters(
            responseType = "TODO",
            clientId = "TODO",
            responseMode = OpenIdConstants.ResponseMode.DirectPost,
            responseUrl = "Must be present if direct post",
            nonce = uuid4().toString(),
            documentDigests = listOf(
                OAuthDocumentDigest(
                    hash = "abc".decodeBase64Bytes(), label = "efg"
                )
            ),
            documentLocations = listOf(
                DocumentLocation(
                    uri = "https://www.google.com", method = Method.Public
                )
            ),
            clientData = null
        )
        sign(testRequest, clientId)
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
        val rqesRequest = vckJsonSerializer.decodeFromString<SignatureRequestParameters>(bodyDecoded)


        Napier.d { "${test.status} with $rqesRequest" }
        sign(rqesRequest, clientId)

    }

    private suspend fun sign(request: SignatureRequestParameters, clientId: String) {

        val authRequest = rqesWalletService.createOAuth2AuthenticationRequest(
            request,
            "1234".toByteArray() //TODO
        )
        rqesMutex[authRequest.state ?: throw Exception("no state")] = RqesEntry(clientId, request, authRequest.authorizationDetails!!.first())

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
        val mutexEntry = rqesMutex[state] ?: throw Exception("Cannot find Entry associated with this state")

        val tokenRequest = rqesWalletService.createOAuth2TokenRequest(
            state = state,
            authorizationDetails = setOf(mutexEntry.authorizationDetails),
            authorization = OAuth2Client.AuthorizationForToken.Code(
                test.parameters["code"] ?: throw Exception("Missing authorization")
            )
        )

        val tokenResponse = client.post("$target/oauth2/token") {
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

        /**
         * QTSP requires SAD, which may or may not be correct?
         */
        val signHash = rqesWalletService.createSignHashRequestParameters(
            rqesRequest = mutexEntry.rqesRequest,
            credentialId = "1234", //TODO
            sad = tokenResponseParameters.accessToken
        )

        val signatures = client.post("$target/csc/v2/signatures/signDoc") {
            contentType(Json)
            accept(Json)
            setBody(signHash)
        }

        /**
         * TODO echte data class in VCK
         */
        val body = signatures.body<String>()
        val code = signatures.status
        Napier.d { "$code and $body" }

        val testUrl = URLBuilder(mutexEntry.rqesRequest.responseUrl!!).apply {
            parameters.append("state", state)
            parameters.append("signatureObject", body)
        }.buildString()

        val test111 = client.post(testUrl) {
            contentType(Json)
            accept(Json)
            setBody(signHash)
        }

        val finalredirect = test111.headers["Location"]
        Napier.d { "${test111.status} has redirect $finalredirect" }

    }

}