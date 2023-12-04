package data.storage

import DataStoreService
import Resources
import at.asitplus.wallet.lib.data.jsonSerializer
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.hostIsIp
import io.ktor.http.isSecure
import io.ktor.util.date.getTimeMillis
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.atomicfu.AtomicLong
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlin.math.min

data class CookieContainer(val cookies: MutableList<Cookie>, val oldestCookie: AtomicLong)


// Modified from io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
class PersistentCookieStorage(private val dataStoreService: DataStoreService): CookiesStorage{
    private val container = importFromDataStore()
    private val mutex = Mutex()

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = getTimeMillis()
        if (now >= container.oldestCookie.value) cleanup(now)

        return@withLock container.cookies.filter { it.matches(requestUrl) }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        with(cookie) {
            if (name.isBlank()) return@withLock
        }

        container.cookies.removeAll { it.name == cookie.name && it.matches(requestUrl) }
        container.cookies.add(cookie.fillDefaults(requestUrl))
        cookie.expires?.timestamp?.let { expires ->
            if (container.oldestCookie.value > expires) {
                container.oldestCookie.value = expires
            }
        }
        exportToDataStore()
    }

    override fun close() {
    }

    private fun cleanup(timestamp: Long) {
        container.cookies.removeAll { cookie ->
            val expires = cookie.expires?.timestamp ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = container.cookies.fold(Long.MAX_VALUE) { acc, cookie ->
            cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
        }

        container.oldestCookie.value = newOldest
        exportToDataStore()
    }

    private fun exportToDataStore() {
        runBlocking {
            val json = jsonSerializer.encodeToString(container)
            dataStoreService.setData(key = Resources.DATASTORE_KEY_COOKIES, value = json)
        }
    }

    private fun importFromDataStore(): CookieContainer {
        return runBlocking {
            val input = dataStoreService.getData(Resources.DATASTORE_KEY_COOKIES)
            jsonSerializer.decodeFromString(input.toString())
        }
    }
}


/**
 * Workaround to implement a working CookieStorage
 * https://youtrack.jetbrains.com/issue/KTOR-6119/Persistent-Cookie-Storage-Make-Cookie.matches-and-Cookie.fillDefaults-methods-public
 */
fun Cookie.matches(requestUrl: Url): Boolean {
    val domain = domain?.toLowerCasePreservingASCIIRules()?.trimStart('.')
        ?: error("Domain field should have the default value")

    val path = with(path) {
        val current = path ?: error("Path field should have the default value")
        if (current.endsWith('/')) current else "$path/"
    }

    val host = requestUrl.host.toLowerCasePreservingASCIIRules()
    val requestPath = let {
        val pathInRequest = requestUrl.encodedPath
        if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
    }

    if (host != domain && (hostIsIp(host) || !host.endsWith(".$domain"))) {
        return false
    }

    if (path != "/" &&
        requestPath != path &&
        !requestPath.startsWith(path)
    ) {
        return false
    }

    return !(secure && !requestUrl.protocol.isSecure())
}
fun Cookie.fillDefaults(requestUrl: Url): Cookie {
    var result = this

    if (result.path?.startsWith("/") != true) {
        result = result.copy(path = requestUrl.encodedPath)
    }

    if (result.domain.isNullOrBlank()) {
        result = result.copy(domain = requestUrl.host)
    }

    return result
}