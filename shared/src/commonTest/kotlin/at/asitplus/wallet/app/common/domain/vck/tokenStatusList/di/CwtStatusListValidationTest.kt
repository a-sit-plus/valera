package at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di

import at.asitplus.signum.indispensable.cosef.CoseHeader
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.HttpFailureCache
import at.asitplus.wallet.app.common.data.primitives.CacheStoreEntry
import at.asitplus.wallet.lib.DefaultZlibService
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.TrustedCertificates
import at.asitplus.wallet.lib.cbor.CoseHeaderCertificate
import at.asitplus.wallet.lib.cbor.SignCose
import at.asitplus.wallet.lib.data.StatusListCwt
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.IdentifierList
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.IdentifierListInfo
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.MediaTypes
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.RevocationList
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListInfo
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListView
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.TokenStatusInfo
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.iso18013.Identifier
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.iso18013.IdentifierInfo
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.PositiveDuration
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusBitSize
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import at.asitplus.wallet.lib.extensions.toStatusList
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.StatusListTokenChecks
import at.asitplus.wallet.lib.validation.StatusPolicy
import at.asitplus.wallet.lib.validation.StatusSignerAnchors
import at.asitplus.wallet.lib.validation.StatusValidator
import at.asitplus.wallet.lib.validation.TrustPolicy
import data.storage.DummyDataStoreService
import data.storage.PersistentHttpCacheStorage
import data.storage.PersistentStatusListTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * End to end: a CWT status list token is requested through a client configured like
 * [at.asitplus.wallet.app.common.HttpService.cachedResourceClient], parsed, persisted, and validated by VC-K.
 */
class CwtStatusListValidationTest {
    private val statusUri = "https://issuer.example.test/status/1"
    private val identifierUri = "https://issuer.example.test/identifiers/1"
    private val signer = EphemeralKeyWithSelfSignedCert()
    private val anchors = StatusSignerAnchors.OfArtifactKind(TrustedCertificates { setOf(signer.getCertificate()!!) }, "test")
    private val policy = StatusPolicy.RequireClaim(TrustPolicy.RequireAuthorizedSigner)

    /** Two bits per status: index 0 valid, index 1 invalid, index 2 suspended. */
    private val statusList = StatusListView(byteArrayOf(0b0010_0100), TokenStatusBitSize.TWO)
        .toStatusList(DefaultZlibService(), statusListAggregationUrl = null)
    private val revokedIdentifier = byteArrayOf(7)

    private suspend fun cwt(revocationList: RevocationList, subject: String, type: String) = StatusListCwt(
        SignCose<ByteArray>(signer, unprotectedHeaderModifier = CoseHeaderCertificate()).invoke(
            CoseHeader(type = type),
            null,
            coseCompliantSerializer.encodeToByteArray(
                StatusListTokenPayload(
                    subject = UniformResourceIdentifier(subject),
                    issuedAt = Clock.System.now(),
                    expirationTime = Clock.System.now() + 1.hours,
                    timeToLive = PositiveDuration(1.hours),
                    revocationList = revocationList,
                )
            ),
            ByteArraySerializer(),
        ).getOrThrow(),
        resolvedAt = null,
    )

    private class Server(val tokens: Map<String, Pair<String, ByteArray>>) {
        val responses = mutableListOf<HttpStatusCode>()
        val engine = MockEngine { request ->
            val (type, body) = tokens.getValue(request.url.toString())
            val etag = "\"${body.contentHashCode()}\""
            if (request.headers[HttpHeaders.IfNoneMatch] == etag) {
                responses += HttpStatusCode.NotModified
                respond(ByteArray(0), HttpStatusCode.NotModified, headersOf(HttpHeaders.ETag, etag))
            } else {
                responses += HttpStatusCode.OK
                respond(
                    body,
                    HttpStatusCode.OK,
                    headersOf(
                        HttpHeaders.ContentType to listOf(type),
                        HttpHeaders.ETag to listOf(etag),
                        HttpHeaders.CacheControl to listOf("max-age=3600"),
                    ),
                )
            }
        }
    }

    /** Mirrors HttpService.buildHttpClient for the revalidating cached resource client, except the engine. */
    private fun client(server: Server) = HttpClient(server.engine) {
        followRedirects = false
        install(ContentNegotiation) { json(joseCompliantSerializer) }
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.CacheControl, "no-cache")
        }
        install(HttpCache) {
            isShared = true
            publicStorage(PersistentHttpCacheStorage(DummyDataStoreService(), Configuration.DATASTORE_KEY_HTTP_CACHE))
            privateStorage(CacheStorage.Disabled)
        }
        install(HttpFailureCache) { cooldown = Configuration.HTTP_FAILURE_COOLDOWN }
    }

    private suspend fun validate(status: TokenStatusInfo, token: suspend (UniformResourceIdentifier) -> StatusListToken) =
        StatusValidator({ token(it) }).validate(status, anchors, policy, 5.minutes, Clock.System.now())

    @Test
    fun validatesAStatusListCwtFromTheServerAlsoAfterRevalidation() = runTest {
        val token = cwt(statusList, statusUri, MediaTypes.Application.STATUSLIST_CWT)
        val server = Server(mapOf(statusUri to (MediaTypes.Application.STATUSLIST_CWT to token.encodeForPublication())))
        val client = client(server)

        listOf(0 to TokenStatus.Valid, 1 to TokenStatus.Invalid, 2 to TokenStatus.Suspended).forEach { (index, expected) ->
            val status = TokenStatusInfo(statusList = StatusListInfo(index.toULong(), UniformResourceIdentifier(statusUri)))
            val validation = validate(status) {
                assertIs<StatusListCwt>(client.resolveStatusListToken(it, ignoreFailureCooldown = false))
            }
            val mechanism = validation.mechanisms.single()
            assertEquals(expected, mechanism.observed)
            assertEquals(CheckOutcome.Passed, assertIs<StatusListTokenChecks>(mechanism.token.checks).signerTrust.outcome)
            assertEquals(expected == TokenStatus.Valid, mechanism.outcome == CheckOutcome.Passed)
        }
        // The first request fetches the token, the others are answered from the cache after a 304
        assertEquals(listOf(HttpStatusCode.OK, HttpStatusCode.NotModified, HttpStatusCode.NotModified), server.responses)
    }

    @Test
    fun validatesAStatusListAndAnIdentifierListCwtOfAnMdoc() = runTest {
        val identifierList = IdentifierList(mapOf(Identifier(revokedIdentifier) to IdentifierInfo()))
        val server = Server(
            mapOf(
                statusUri to (MediaTypes.Application.STATUSLIST_CWT to
                        cwt(statusList, statusUri, MediaTypes.Application.STATUSLIST_CWT).encodeForPublication()),
                identifierUri to (MediaTypes.Application.IDENTIFIERLIST_CWT to
                        cwt(identifierList, identifierUri, MediaTypes.Application.IDENTIFIERLIST_CWT).encodeForPublication()),
            )
        )
        val client = client(server)
        val resolve: suspend (UniformResourceIdentifier) -> StatusListToken =
            { client.resolveStatusListToken(it, ignoreFailureCooldown = false) }

        val valid = validate(
            TokenStatusInfo(
                statusList = StatusListInfo(0u, UniformResourceIdentifier(statusUri)),
                identifierList = IdentifierListInfo(byteArrayOf(1), UniformResourceIdentifier(identifierUri)),
            ),
            resolve,
        )
        assertEquals(listOf(CheckOutcome.Passed, CheckOutcome.Passed), valid.mechanisms.map { it.outcome })
        assertEquals(CheckOutcome.Passed, valid.agreement)

        val revoked = validate(
            TokenStatusInfo(identifierList = IdentifierListInfo(revokedIdentifier, UniformResourceIdentifier(identifierUri))),
            resolve,
        )
        assertEquals(TokenStatus.Invalid, revoked.mechanisms.single().observed)
    }

    @Test
    fun validatesAPersistedStatusListCwt() = runTest {
        val dataStore = DummyDataStoreService()
        val uri = UniformResourceIdentifier(statusUri)
        val token = cwt(statusList, statusUri, MediaTypes.Application.STATUSLIST_CWT).copy(resolvedAt = Clock.System.now())
        PersistentStatusListTokenStore(dataStore).set(uri, CacheStoreEntry(token, Clock.System.now()))

        val restored = PersistentStatusListTokenStore(dataStore).get(uri)!!.data
        assertIs<StatusListCwt>(restored)
        val validation = validate(TokenStatusInfo(statusList = StatusListInfo(0u, uri))) { restored }
        assertEquals(CheckOutcome.Passed, validation.mechanisms.single().outcome)
    }
}
