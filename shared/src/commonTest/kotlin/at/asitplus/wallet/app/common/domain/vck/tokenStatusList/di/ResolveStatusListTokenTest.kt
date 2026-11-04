package at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di

import at.asitplus.wallet.lib.agent.StatusListAgent
import at.asitplus.wallet.lib.data.StatusListCwt
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.IdentifierList
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.MediaTypes
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.RevocationList
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusList
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ResolveStatusListTokenTest {
    private val url = UniformResourceIdentifier("https://example.test/status")

    private fun client(contentType: String, status: HttpStatusCode = HttpStatusCode.OK, body: () -> ByteArray) =
        HttpClient(MockEngine { respond(body(), status, headersOf(HttpHeaders.ContentType, contentType)) })

    @Test
    fun resolvesATaggedIdentifierListCwt() = runTest {
        val cwt = StatusListCwt(StatusListAgent().issueStatusListCwt(kind = RevocationList.Kind.IDENTIFIER_LIST), null)
        val client = client(MediaTypes.Application.IDENTIFIERLIST_CWT) { cwt.encodeForPublication() }

        val token = assertIs<StatusListCwt>(client.resolveStatusListToken(url, ignoreFailureCooldown = false))

        assertEquals(cwt.value, token.value)
        assertIs<IdentifierList>(token.parsedPayload.getOrThrow().revocationList)
    }

    @Test
    fun resolvesAStatusListCwt() = runTest {
        val cwt = StatusListCwt(StatusListAgent().issueStatusListCwt(), null)
        val client = client("${MediaTypes.Application.STATUSLIST_CWT}; charset=binary") { cwt.encodeForPublication() }

        val token = assertIs<StatusListCwt>(client.resolveStatusListToken(url, ignoreFailureCooldown = false))

        assertIs<StatusList>(token.parsedPayload.getOrThrow().revocationList)
    }

    @Test
    fun resolvesAJwtAndAcceptsEveryFormat() = runTest {
        val jws = StatusListAgent().issueStatusListJwt()
        var accept: String? = null
        val client = HttpClient(MockEngine { request ->
            accept = request.headers[HttpHeaders.Accept]
            respond(
                jws.toString(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, MediaTypes.Application.STATUSLIST_JWT),
            )
        })

        val token = assertIs<StatusListJwt>(client.resolveStatusListToken(url, ignoreFailureCooldown = false))

        assertEquals(jws.toString(), token.value.toString())
        listOf(
            MediaTypes.Application.STATUSLIST_JWT,
            MediaTypes.Application.STATUSLIST_CWT,
            MediaTypes.Application.IDENTIFIERLIST_CWT,
        ).forEach { assertContains(accept.orEmpty(), it) }
    }

    @Test
    fun rejectsAnErrorResponse() = runTest {
        val client = client("text/html", HttpStatusCode.NotAcceptable) { "<html></html>".encodeToByteArray() }

        val error = assertFailsWith<IllegalArgumentException> {
            client.resolveStatusListToken(url, ignoreFailureCooldown = false)
        }

        assertContains(error.message.orEmpty(), "406")
    }

    @Test
    fun namesTheTypeOfAResponseThatIsNoToken() = runTest {
        val client = client("text/html") { "<html></html>".encodeToByteArray() }

        val error = assertFailsWith<IllegalArgumentException> {
            client.resolveStatusListToken(url, ignoreFailureCooldown = false)
        }

        assertContains(error.message.orEmpty(), "text/html")
    }
}
