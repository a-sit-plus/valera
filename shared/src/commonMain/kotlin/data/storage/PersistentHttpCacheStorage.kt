package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.Configuration
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

private val persistentHttpCacheMutex = Mutex()

/**
 * DataStore-backed Ktor [CacheStorage] so validators (ETag/Last-Modified) and bodies survive app
 * restarts — that is what makes the conditional GET answer with `304 Not Modified` on a cold launch.
 * Entries for a URL are stored as a list, one per `varyKeys` combination.
 */
class PersistentHttpCacheStorage(
    dataStoreService: DataStoreService,
    preferenceKey: String,
) : CacheStorage {
    // ponytail: the whole cache is one DataStore blob (≤ MAX_TOTAL_BODY_BYTES) re-parsed on every find/store;
    // Ktor calls find per GET and find/findAll/store per 304-refresh. Fine while fetches are TTL-gated and
    // off-main. If cache-parse latency ever shows up, re-key per URL so each op touches only that URL's blob.
    private val store = PersistentSimpleStore<String, List<StoredCacheEntry>>(
        dataStoreService = dataStoreService,
        preferenceKey = preferenceKey,
    )

    override suspend fun store(url: Url, data: CachedResponseData): Unit = persistentHttpCacheMutex.withLock {
        val key = url.toString()
        val others = store[key].orEmpty().filterNot { it.varyKeys == data.varyKeys }
        val updated = if (data.body.size <= MAX_ENTRY_BYTES) others + data.toStored() else others
        if (updated.isEmpty()) store.remove(key) else store[key] = updated
        evictLocked()
    }

    override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? =
        store[url.toString()]?.firstOrNull { it.varyKeys == varyKeys }?.toData(url)

    override suspend fun findAll(url: Url): Set<CachedResponseData> =
        store[url.toString()].orEmpty().mapNotNull { it.toData(url) }.toSet()

    override suspend fun remove(url: Url, varyKeys: Map<String, String>): Unit =
        persistentHttpCacheMutex.withLock {
            val key = url.toString()
            val remaining = store[key].orEmpty().filterNot { it.varyKeys == varyKeys }
            if (remaining.isEmpty()) store.remove(key) else store[key] = remaining
        }

    override suspend fun removeAll(url: Url): Unit = persistentHttpCacheMutex.withLock {
        store.remove(url.toString())
        Unit
    }

    private suspend fun evictLocked() {
        val entries = store.entries().mapValuesTo(mutableMapOf()) { it.value.toMutableList() }
        // ponytail: bounded O(n) eviction is simpler than maintaining a second persistent LRU index.
        while (entries.entryCount() > MAX_ENTRIES || entries.totalBodySize() > MAX_TOTAL_BODY_BYTES) {
            val oldest = entries.entries
                .flatMap { (url, responses) -> responses.map { url to it } }
                .minByOrNull { it.second.responseTimeMillis }
                ?: break
            val remaining = entries.getValue(oldest.first).apply { remove(oldest.second) }
            if (remaining.isEmpty()) {
                entries.remove(oldest.first)
                store.remove(oldest.first)
            } else {
                store[oldest.first] = remaining
            }
        }
    }

    private fun CachedResponseData.toStored() = StoredCacheEntry(
        statusCode = statusCode.value,
        requestTimeMillis = requestTime.timestamp,
        responseTimeMillis = responseTime.timestamp,
        expiresMillis = expires.timestamp,
        versionName = version.name,
        versionMajor = version.major,
        versionMinor = version.minor,
        headers = headers.toMap(),
        varyKeys = varyKeys,
        bodyBase64 = Base64.encode(body),
    )

    private fun StoredCacheEntry.toData(url: Url): CachedResponseData? = catchingUnwrapped {
        CachedResponseData(
            url = url,
            statusCode = HttpStatusCode.fromValue(statusCode),
            requestTime = GMTDate(requestTimeMillis),
            responseTime = GMTDate(responseTimeMillis),
            version = HttpProtocolVersion(versionName, versionMajor, versionMinor),
            expires = GMTDate(expiresMillis),
            headers = HeadersBuilder().apply { headers.forEach { (name, values) -> appendAll(name, values) } }.build(),
            varyKeys = varyKeys,
            body = Base64.decode(bodyBase64),
        )
    }.onFailure {
        Napier.w("Ignoring invalid cached HTTP response for $url", it)
    }.getOrNull()

    private fun Map<String, List<StoredCacheEntry>>.entryCount() = values.sumOf { it.size }
    private fun Map<String, List<StoredCacheEntry>>.totalBodySize() =
        values.sumOf { responses -> responses.sumOf { it.bodySize } }

    private companion object {
        const val MAX_ENTRIES = Configuration.MAX_PERSISTENT_CACHE_ENTRIES
        const val MAX_ENTRY_BYTES = 1024 * 1024
        const val MAX_TOTAL_BODY_BYTES = 4 * 1024 * 1024
    }
}

@Serializable
private data class StoredCacheEntry(
    val statusCode: Int,
    val requestTimeMillis: Long,
    val responseTimeMillis: Long,
    val expiresMillis: Long,
    val versionName: String,
    val versionMajor: Int,
    val versionMinor: Int,
    val headers: Map<String, List<String>>,
    val varyKeys: Map<String, String>,
    val bodyBase64: String,
) {
    val bodySize: Int get() = bodyBase64.length * 3 / 4
}
