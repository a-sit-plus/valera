package data.storage

import at.asitplus.wallet.app.common.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistentHttpCacheStorageTest {
    private val url = Url("https://example.test/resource.json")
    private val varyKeys = mapOf("Accept" to "application/json")

    private fun storage(dataStore: DataStoreService) =
        PersistentHttpCacheStorage(dataStore, Configuration.DATASTORE_KEY_HTTP_CACHE)

    private fun entry(
        url: Url = this.url,
        body: ByteArray,
        responseTime: Long = 2_000L,
    ) = CachedResponseData(
        url = url,
        statusCode = HttpStatusCode.OK,
        requestTime = GMTDate(1_000L),
        responseTime = GMTDate(responseTime),
        version = HttpProtocolVersion.HTTP_1_1,
        expires = GMTDate(3_000L),
        headers = headersOf("ETag", "\"abc\""),
        varyKeys = varyKeys,
        body = body,
    )

    @Test
    fun storesAndFindsAcrossInstances() = runTest {
        val dataStore = DummyDataStoreService()
        storage(dataStore).store(url, entry(body = "hello".encodeToByteArray()))

        // A fresh instance (i.e. after a restart) reads what the first one persisted.
        val found = storage(dataStore).find(url, varyKeys)
        assertEquals(HttpStatusCode.OK, found?.statusCode)
        assertEquals("\"abc\"", found?.headers?.get("ETag"))
        assertEquals(3_000L, found?.expires?.timestamp)
        assertContentEquals("hello".encodeToByteArray(), found?.body)

        assertEquals(1, storage(dataStore).findAll(url).size)
        // varyKeys must match to be a hit.
        assertNull(storage(dataStore).find(url, emptyMap()))
    }

    @Test
    fun removesEntries() = runTest {
        val dataStore = DummyDataStoreService()
        storage(dataStore).store(url, entry(body = "x".encodeToByteArray()))
        storage(dataStore).remove(url, varyKeys)
        assertNull(storage(dataStore).find(url, varyKeys))

        storage(dataStore).store(url, entry(body = "y".encodeToByteArray()))
        storage(dataStore).removeAll(url)
        assertTrue(storage(dataStore).findAll(url).isEmpty())
    }

    @Test
    fun ignoresMalformedPersistedBlob() = runTest {
        val dataStore = DummyDataStoreService()
        dataStore.setPreference("not json", Configuration.DATASTORE_KEY_HTTP_CACHE)
        assertTrue(storage(dataStore).findAll(url).isEmpty())
    }

    @Test
    fun preservesEntriesWrittenByDifferentInstances() = runTest {
        val dataStore = DummyDataStoreService()
        val first = storage(dataStore)
        val second = storage(dataStore)
        val otherUrl = Url("https://example.test/other.json")

        first.findAll(url)
        second.findAll(otherUrl)
        first.store(url, entry(body = "first".encodeToByteArray()))
        second.store(otherUrl, entry(url = otherUrl, body = "second".encodeToByteArray()))

        assertNotNull(storage(dataStore).find(url, varyKeys))
        assertNotNull(storage(dataStore).find(otherUrl, varyKeys))
    }

    @Test
    fun reusesPersistedBodyAfterServerReturnsNotModified() = runTest {
        val dataStore = DummyDataStoreService()
        var requestCount = 0
        fun client(revalidate: Boolean = false) = HttpClient(MockEngine { request ->
            when (++requestCount) {
                1 -> respond(
                    content = "cached body",
                    headers = Headers.build {
                        append(HttpHeaders.ETag, "\"v1\"")
                        append(HttpHeaders.CacheControl, "max-age=604800")
                    },
                )

                else -> {
                    assertEquals("\"v1\"", request.headers[HttpHeaders.IfNoneMatch])
                    respond(
                        content = "",
                        status = HttpStatusCode.NotModified,
                        headers = headersOf(HttpHeaders.ETag, "\"v1\""),
                    )
                }
            }
        }) {
            if (revalidate) {
                install(DefaultRequest) {
                    header(HttpHeaders.CacheControl, "no-cache")
                }
            }
            install(HttpCache) {
                isShared = true
                publicStorage(storage(dataStore))
                privateStorage(CacheStorage.Disabled)
            }
        }

        client(revalidate = true).use { assertEquals("cached body", it.get(url).bodyAsText()) }
        client(revalidate = true).use { assertEquals("cached body", it.get(url).bodyAsText()) }
        assertEquals(2, requestCount)
    }

    @Test
    fun boundsPersistedResponses() = runTest {
        val dataStore = DummyDataStoreService()
        val storage = storage(dataStore)

        storage.store(url, entry(body = ByteArray(1024 * 1024 + 1)))
        assertNull(storage.find(url, varyKeys))

        repeat(65) { index ->
            val indexedUrl = Url("https://example.test/$index")
            storage.store(
                indexedUrl,
                entry(url = indexedUrl, body = byteArrayOf(index.toByte()), responseTime = index.toLong()),
            )
        }
        assertNull(storage.find(Url("https://example.test/0"), varyKeys))
        assertNotNull(storage.find(Url("https://example.test/64"), varyKeys))
    }
}
