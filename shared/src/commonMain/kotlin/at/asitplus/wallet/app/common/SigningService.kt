package at.asitplus.wallet.app.common

import at.asitplus.dif.rqes.DocumentLocationEntry
import at.asitplus.dif.rqes.Method
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.DocumentDigestCSCEntry
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.encodeToJsonElement


class SigningService(
    val platformAdapter: PlatformAdapter,
    httpService: HttpService
) {
    private val client = httpService.buildHttpClient()

//    private val target = "https://apps.egiz.gv.at/qtsp"
    private val target = "http://127.0.0.1:7086/qtsp"
    private val clientId = "https://wallet.a-sit.at/mobile"

    private val testDigest = DocumentDigestCSCEntry(
        hash = "abc".decodeBase64Bytes(), label = "efg"
    )
    private val testLocation = DocumentLocationEntry(
        uri = Url("https://www.google.com"), method = Method.Public
    )

    private val cscCredential = AuthorizationDetails.CSCCredential(
        documentDigestsCSC = setOf(testDigest),
        documentLocations = setOf(testLocation),
        hashAlgorithmOID = ObjectIdentifier("2.16.840.1.101.3.4.2.1"),
        locations = null
    )
    private val walletService = WalletService(
        clientId = clientId,
        redirectUrl = clientId
    )

    suspend fun sign() {
        println("SigningService: sign()")

        val authRequest =
            walletService.createAuthRequest(
                state = "asdf",
                authorizationDetails = cscCredential
            )

        val presentationRequest = kotlin.runCatching {
            client.get("$target/oauth2/authorize") {
                authRequest.encodeToParameters().forEach {
                    this.parameter(it.key, it.value)
                }
            }
        }.getOrElse { throw Exception("Did not receive presentation request from Q") }

        Napier.w("We received a request poggers $presentationRequest")
        platformAdapter.openUrl(presentationRequest.headers[HttpHeaders.Location] ?: throw Exception("codeUrl is null"))
    }

    fun sign(byteArray: ByteArray) {
        println("SigningService: sign " + byteArray.size.toString())
    }

    //TODO rename function
    suspend fun oauth2TokenAfterSuccessfulPresentationCSC(location: String) {
        val test = Url(location)

        val tokenRequest = walletService.createTokenRequestParameters(
            state = "asdf",
            authorizationDetails = cscCredential,
            authorization = WalletService.AuthorizationForToken.Code(
                test.parameters["code"] ?: throw Exception("Missing authorization")
            )
        )
        val tokenResponse = client.post("$target/oauth2/token") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                tokenRequest.encodeToParameters().formUrlEncode()
            )
        }
        Napier.d { tokenResponse.toString() }
    }

}
