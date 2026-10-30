package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.io.encoding.Base64

private val persistentHttpCacheMutex = Mutex()

/**
 * DataStore-backed Ktor [CacheStorage] so validators (ETag/Last-Modified) and bodies survive app
 * restarts — that is what makes the conditional GET answer with `304 Not Modified` on a cold launch.
 * The entries of a URL are a preference of their own, one per `varyKeys` combination, so that a request decodes and
 * encodes only those of its URL, not every cached Trust List. A small index of every URL bounds them, see
 * [evictLocked].
 *
 * The other iOS process may write between an entry and the index: a URL in the index without entries is a miss, and
 * entries missing from the index are replaced by the next response of their URL.
 */
class PersistentHttpCacheStorage(
    private val dataStoreService: DataStoreService,
    private val preferenceKey: String,
) : CacheStorage {
    private val index = PersistentSimpleStore<String, IndexEntry>(
        dataStoreService = dataStoreService,
        preferenceKey = "${preferenceKey}_index",
    )

    /** Whether the blob that held every response before they were stored per URL was removed, see [store]. */
    private var legacyBlobRemoved = false

    override suspend fun store(url: Url, data: CachedResponseData): Unit = persistentHttpCacheMutex.withLock {
        removeLegacyBlobLocked()
        val key = url.toString()
        val others = load(key).filterNot { it.varyKeys == data.varyKeys }
        val updated = if (data.body.size <= MAX_ENTRY_BYTES) others + data.toStored() else others
        saveLocked(key, updated)
        evictLocked()
    }

    override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? =
        load(url.toString()).firstOrNull { it.varyKeys == varyKeys }?.toData(url)

    override suspend fun findAll(url: Url): Set<CachedResponseData> =
        load(url.toString()).mapNotNull { it.toData(url) }.toSet()

    override suspend fun remove(url: Url, varyKeys: Map<String, String>): Unit =
        persistentHttpCacheMutex.withLock {
            val key = url.toString()
            saveLocked(key, load(key).filterNot { it.varyKeys == varyKeys })
        }

    override suspend fun removeAll(url: Url): Unit = persistentHttpCacheMutex.withLock {
        saveLocked(url.toString(), emptyList())
    }

    private fun entriesKey(url: String) = "$preferenceKey|$url"

    private suspend fun load(url: String): List<StoredCacheEntry> = catchingUnwrapped {
        dataStoreService.getPreference(entriesKey(url)).first()
            ?.let { joseCompliantSerializer.decodeFromString(entriesSerializer, it) }
            .orEmpty()
    }.onFailure {
        Napier.w("Ignoring invalid cached HTTP responses for $url", it)
    }.getOrDefault(emptyList())

    private suspend fun saveLocked(url: String, entries: List<StoredCacheEntry>) {
        catchingUnwrapped {
            if (entries.isEmpty()) {
                dataStoreService.deletePreferenceIfPresent(entriesKey(url))
                index.remove(url)
            } else {
                dataStoreService.setPreference(
                    joseCompliantSerializer.encodeToString(entriesSerializer, entries),
                    entriesKey(url),
                )
                index[url] = IndexEntry(
                    entryCount = entries.size,
                    bodySize = entries.sumOf { it.bodySize },
                    responseTimeMillis = entries.maxOf { it.responseTimeMillis },
                )
            }
        }.onFailure {
            Napier.w("Could not persist cached HTTP responses for $url", it)
        }
    }

    /** Drops the URLs responded to longest ago until the entries and their bodies fit the limits. */
    private suspend fun evictLocked() {
        val entries = index.entries().toMutableMap()
        // ponytail: bounded O(n) eviction over the small index is simpler than a persistent LRU order.
        while (entries.values.sumOf { it.entryCount } > MAX_ENTRIES ||
            entries.values.sumOf { it.bodySize } > MAX_TOTAL_BODY_BYTES
        ) {
            val oldest = entries.minByOrNull { it.value.responseTimeMillis }?.key ?: break
            entries.remove(oldest)
            saveLocked(oldest, emptyList())
        }
    }

    private suspend fun removeLegacyBlobLocked() {
        if (legacyBlobRemoved) return
        catchingUnwrapped { dataStoreService.deletePreferenceIfPresent(preferenceKey) }
            .onSuccess { legacyBlobRemoved = true }
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

    private companion object {
        val entriesSerializer = ListSerializer(StoredCacheEntry.serializer())

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

@Serializable
private data class IndexEntry(
    val entryCount: Int,
    val bodySize: Int,
    /** The latest response of the URL, which [PersistentHttpCacheStorage] evicts the oldest of first. */
    val responseTimeMillis: Long,
)
