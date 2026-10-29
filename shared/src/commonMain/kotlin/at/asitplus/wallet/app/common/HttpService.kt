package at.asitplus.wallet.app.common

import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import data.storage.DataStoreService
import data.storage.PersistentHttpCacheStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.BinaryLogBodyFilter
import io.ktor.client.plugins.logging.BodyFilterResult
import io.ktor.client.plugins.logging.LogBodyFilter
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.discard
import io.ktor.utils.io.readRemaining
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.io.readByteArray
import kotlin.time.Duration

/**
 * Central place to build [HttpClient]
 */
class HttpService(private val buildContext: BuildContext) {
    private val resourceClientLock = SynchronizedObject()
    private var resourceClient: HttpClient? = null
    private var revalidatingResourceClient: HttpClient? = null

    /**
     * Builds a client with the platform engine. Cookies are only stored and sent with [cookieStorage], e.g. for the
     * session of an issuer or a signing service: without one, the client sends no cookies, so that a verifier can not
     * link presentations, nor a status list provider the status checks of this wallet, by a session cookie.
     */
    fun buildHttpClient(
        cookieStorage: CookiesStorage? = null,
        cacheStorage: CacheStorage? = null,
        revalidateCachedResponses: Boolean = false,
        failureCooldown: Duration? = null,
    ) = HttpClient(createPlatformHttpClientEngine()) {
        followRedirects = false
        install(ContentNegotiation) {
            json(joseCompliantSerializer)
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            if (revalidateCachedResponses) {
                header(HttpHeaders.CacheControl, "no-cache")
            }
        }
        apply(loggingConfig)
        cookieStorage?.let {
            install(HttpCookies) {
                storage = it
            }
        }
        // Conditional GET: the plugin sends If-None-Match/If-Modified-Since and handles 304 transparently.
        cacheStorage?.let { storage ->
            install(HttpCache) {
                isShared = true
                publicStorage(storage)
                privateStorage(CacheStorage.Disabled)
            }
        }
        // Negative cache: after an error status, stop re-fetching that URL until the cooldown elapses.
        failureCooldown?.let { cooldown ->
            install(HttpFailureCache) { this.cooldown = cooldown }
        }
    }

    /**
     * Process-wide client for public resources. [revalidate] forces a conditional request even while the server's
     * cached response is fresh, which keeps application-specific TTLs authoritative.
     *
     * Note: the [HttpCache] here only turns a *network* fetch into a cheap `304`; with [revalidate] it never serves
     * a response offline. Offline resilience is intentionally handled by the app-level stores (metadata
     * stale-fallback, trust-list last-good filtered by its TTL, token-status store) — don't add offline fallback here.
     */
    fun cachedResourceClient(dataStoreService: DataStoreService, revalidate: Boolean = false): HttpClient =
        synchronized(resourceClientLock) {
            if (revalidate) {
                revalidatingResourceClient ?: buildCachedResourceClient(dataStoreService, true)
                    .also { revalidatingResourceClient = it }
            } else {
                resourceClient ?: buildCachedResourceClient(dataStoreService, false).also { resourceClient = it }
            }
        }

    private fun buildCachedResourceClient(dataStoreService: DataStoreService, revalidate: Boolean) =
        buildHttpClient(
            cacheStorage = PersistentHttpCacheStorage(
                dataStoreService,
                Configuration.DATASTORE_KEY_HTTP_CACHE,
            ),
            revalidateCachedResponses = revalidate,
            failureCooldown = Configuration.HTTP_FAILURE_COOLDOWN,
        )

    // Logging a body buffers it and copies it into every log, which takes megabytes for a Trust List, e.g. of the
    // memory of the iOS Identity Document Provider extension
    private val logLevel =
        if (buildContext.buildType == BuildType.RELEASE || isAppExtension()) LogLevel.HEADERS else LogLevel.ALL

    val loggingConfig: HttpClientConfig<*>.() -> Unit
        get() = {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.i(message, tag = "HTTP")
                    }
                }
                level = logLevel
                // Only this format applies the bodyFilter
                format = LoggingFormat.OkHttp
                bodyFilter = TruncatingResponseBodyFilter
            }
            install(DefaultRequest) {
                header(HttpHeaders.UserAgent, "Valera/${buildContext.versionName} (${buildContext.osVersion})")
            }
        }
}

/**
 * Logs only the first [MAX_LOGGED_RESPONSE_BODY_BYTES] of a response body, so that large documents like Lists of
 * Trusted Entities don't flood the log. Request bodies are logged in full.
 */
private object TruncatingResponseBodyFilter : LogBodyFilter by BinaryLogBodyFilter {
    private const val MAX_LOGGED_RESPONSE_BODY_BYTES = 5 * 1024L

    override suspend fun filterResponse(
        url: Url,
        contentLength: Long?,
        contentType: ContentType?,
        headers: Headers,
        body: ByteReadChannel,
    ): BodyFilterResult {
        val head = body.readRemaining(MAX_LOGGED_RESPONSE_BODY_BYTES).readByteArray()
        // Drain the logged copy of the body without buffering it
        val omitted = body.discard()
        val result = BinaryLogBodyFilter.filterResponse(url, contentLength, contentType, headers, ByteReadChannel(head))
        if (omitted == 0L || result !is BodyFilterResult.Content) return result
        val text = result.read()
        return object : BodyFilterResult.Content {
            override val byteSize = head.size + omitted
            override fun read() = "$text\n[... $omitted more bytes omitted]"
        }
    }
}
