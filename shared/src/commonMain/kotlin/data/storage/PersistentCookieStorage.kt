package data.storage

import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.http.hostIsIp
import io.ktor.http.isSecure
import io.ktor.util.date.GMTDate
import io.ktor.util.date.getTimeMillis
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.jvm.JvmName
import kotlin.math.min

// Modified from io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
class PersistentCookieStorage(
    private val dataStoreService: DataStoreService,
    private val errorService: ErrorService
) : CookiesStorage {
    private val container = importFromDataStore()
    private val mutex = Mutex()

    suspend fun reset() = mutex.withLock {
        container.cookies.clear()
        container.oldestCookie.value = 0L
        exportToDataStore()
    }

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
        try {
            val exportableCookies = container.cookies.toExportableCookieList()
            val export = ExportableCookieContainer(
                cookies = exportableCookies,
                oldestCookie = container.oldestCookie.value
            )
            val json = vckJsonSerializer.encodeToString(export)
            runBlocking {
                dataStoreService.setPreference(
                    key = Configuration.DATASTORE_KEY_COOKIES,
                    value = json
                )
            }
        } catch (e: Throwable) {
            errorService.emit(e)
        }
    }

    private fun importFromDataStore(): CookieContainer {
        try {
            val input = runBlocking {
                dataStoreService.getPreference(Configuration.DATASTORE_KEY_COOKIES).firstOrNull()
            }
            if (input == null) {
                return CookieContainer(cookies = mutableListOf(), oldestCookie = atomic(0L))
            } else {
                val export: ExportableCookieContainer = vckJsonSerializer.decodeFromString(input)
                return CookieContainer(
                    cookies = export.cookies.toCookieList(),
                    oldestCookie = atomic(export.oldestCookie)
                )
            }
        } catch (e: Throwable) {
            errorService.emit(e)
        }
        return CookieContainer(cookies = mutableListOf(), oldestCookie = atomic(0L))
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

@Serializable
data class ExportableCookie(
    val name: String,
    val value: String,
    val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
    @get:JvmName("getMaxAgeInt")
    val maxAge: Int = 0,
//    @Contextual val expires: GMTDate? = null,
    val expires: Instant? = null,
    val domain: String? = null,
    val path: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val extensions: Map<String, String?> = emptyMap()
)

data class CookieContainer(val cookies: MutableList<Cookie>, val oldestCookie: AtomicLong)

@Serializable
data class ExportableCookieContainer(
    val cookies: MutableList<ExportableCookie>,
    val oldestCookie: Long
)

fun MutableList<Cookie>.toExportableCookieList(): MutableList<ExportableCookie> {
    return this.map {
        ExportableCookie(
            name = it.name,
            value = it.value,
            encoding = it.encoding,
            maxAge = it.maxAge?:0,
            expires = it.expires?.let { Instant.fromEpochMilliseconds(it.timestamp) },
            domain = it.domain,
            path = it.path,
            secure = false,
            httpOnly = false,
            extensions = it.extensions
        )
    }.toMutableList()
}

fun MutableList<ExportableCookie>.toCookieList(): MutableList<Cookie> {
    return this.map {
        Cookie(
            name = it.name,
            value = it.value,
            encoding = it.encoding,
            maxAge = it.maxAge,
            expires = it.expires?.let { GMTDate(it.toEpochMilliseconds()) },
            domain = it.domain,
            path = it.path,
            secure = false,
            httpOnly = false,
            extensions = it.extensions
        )
    }.toMutableList()
}
