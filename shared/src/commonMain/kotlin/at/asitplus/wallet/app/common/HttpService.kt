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
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Central place to build [HttpClient]
 */
class HttpService(private val buildContext: BuildContext) {
    private val resourceClientLock = SynchronizedObject()
    private var resourceClient: HttpClient? = null
    private var revalidatingResourceClient: HttpClient? = null

    fun buildHttpClient(
        cookieStorage: CookiesStorage? = null,
        cacheStorage: CacheStorage? = null,
        revalidateCachedResponses: Boolean = false,
    ) = HttpClient {
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
        install(HttpCookies) {
            cookieStorage?.let {
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
        )

    val loggingConfig: HttpClientConfig<*>.() -> Unit
        get() = {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.i(message, tag = "HTTP")
                    }
                }
                level = LogLevel.ALL
            }
            install(DefaultRequest) {
                header(HttpHeaders.UserAgent, "Valera/${buildContext.versionName} (${buildContext.osVersion})")
            }
        }
}
