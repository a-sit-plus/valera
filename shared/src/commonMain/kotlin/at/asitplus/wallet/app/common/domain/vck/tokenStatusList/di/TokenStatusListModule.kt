package at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di

import at.asitplus.catching
import at.asitplus.iso.stripCborTag
import at.asitplus.signum.indispensable.cosef.CoseSigned
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.HttpFailureCache
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.IgnoreFailureCooldown
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.data.primitives.CachingStatusListTokenResolver
import at.asitplus.wallet.app.common.data.primitives.SimpleBootstrappingBulkStore
import at.asitplus.wallet.app.common.data.primitives.SimpleCacheStoreWrapper
import at.asitplus.wallet.app.common.domain.vck.di.ISSUANCE
import at.asitplus.wallet.app.common.failOnForeignCancellation
import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListCwt
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.MediaTypes
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import data.storage.DataStoreService
import data.storage.PersistentStatusListTokenStore
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.builtins.ByteArraySerializer
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock


fun tokenStatusListModule() = module {
    scope(named(SESSION_NAME)) {
        scoped<CachingStatusListTokenResolver> {
            val httpService: HttpService by inject()
            val dataStoreService: DataStoreService by inject()
            val client = httpService.cachedResourceClient(dataStoreService, revalidate = true)
            CachingStatusListTokenResolver(
                store = SimpleCacheStoreWrapper(
                    store = SimpleBootstrappingBulkStore(
                        PersistentStatusListTokenStore(dataStoreService),
                    ),
                    clock = Clock.System,
                    getCachingDuration = { (_, value) ->
                        val payload = value.parsedPayload.getOrNull()
                        listOfNotNull(
                            payload?.expirationTime?.let { expiration ->
                                expiration - (value.resolvedAt ?: Clock.System.now())
                            },
                            payload?.timeToLive?.duration
                        ).minOrNull()?.also {
                            Napier.d("Entry specific caching duration is used: $it")
                        } ?: Configuration.CACHE_TTL_TOKEN_STATUS
                    },
                    onEntryFiltered = {
                        // Keep expired entries until a fresh token replaces them.
                    },
                ),
                // Failure backoff for broken status endpoints (404/503) is handled at the HTTP layer by the
                // HttpFailureCache plugin on the shared cached client (see HttpService.buildHttpClient).
                statusListTokenResolver = { client.resolveStatusListToken(it, ignoreFailureCooldown = false) },
                freshStatusListTokenResolver = { client.resolveStatusListToken(it, ignoreFailureCooldown = true) },
            )
        } bind StatusListTokenResolver::class
        scoped<StatusListTokenResolver>(ISSUANCE) { get<CachingStatusListTokenResolver>().fresh }
    }
}

/** Media types of CWT status list tokens: status lists (Token Status List, 5.2) and identifier lists (ISO/IEC 18013-5, 12.3.6.4). */
private val CWT_MEDIA_TYPES = setOf(
    ContentType.parse(MediaTypes.Application.STATUSLIST_CWT),
    ContentType.parse(MediaTypes.Application.IDENTIFIERLIST_CWT),
)

/**
 * Requests the status list token at [url], also while it cools down after an error response if
 * [ignoreFailureCooldown] is set, see [HttpFailureCache]. The issuer chooses the format: a CWT if the response has the
 * media type of one, as for the status of an mdoc, otherwise a JWT.
 *
 * VC-K reports a failed resolution as an unknown status, but rethrows a cancellation, also one of a request whose job
 * was cancelled, which would end validating the credential or relying party.
 */
internal suspend fun HttpClient.resolveStatusListToken(
    url: UniformResourceIdentifier,
    ignoreFailureCooldown: Boolean,
): StatusListToken = failOnForeignCancellation {
    val httpResponse = get(url.string) {
        headers[HttpHeaders.Accept] = listOf(
            MediaTypes.Application.STATUSLIST_JWT,
            MediaTypes.Application.STATUSLIST_CWT,
            MediaTypes.Application.IDENTIFIERLIST_CWT,
        ).joinToString(", ")
        if (ignoreFailureCooldown) attributes.put(IgnoreFailureCooldown, Unit)
    }
    val body = httpResponse.bodyAsBytes()
    require(httpResponse.status.isSuccess()) {
        "Requesting the status list token at $url failed with ${httpResponse.status}"
    }
    val contentType = httpResponse.contentType()
    val resolvedAt = Clock.System.now()
    if (contentType != null && CWT_MEDIA_TYPES.any { contentType.match(it) }) {
        // A CWT status list token is published as a tagged COSE_Sign1 (Token Status List, 5.2)
        StatusListCwt(CoseSigned.deserialize(ByteArraySerializer(), body.stripCborTag(18)).getOrThrow(), resolvedAt)
    } else {
        val jws = catching { JwsCompactTyped<StatusListTokenPayload>(body.decodeToString()) }.getOrElse {
            throw IllegalArgumentException("The status list token of type $contentType is no JWT", it)
        }
        StatusListJwt(jws, resolvedAt)
    }
}
