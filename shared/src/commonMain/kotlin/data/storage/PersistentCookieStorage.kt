package data.storage

import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.ErrorService
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.time.Instant

// Modified from io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
/**
 * Keeps cookies in memory and persists them in the background: Ktor waits for [addCookie] before it hands out the
 * response, and a write to the shared DataStore can take seconds while it is busy, e.g. with caches.
 */
class PersistentCookieStorage(
    private val dataStoreService: DataStoreService,
    private val errorService: ErrorService,
    writeContext: CoroutineContext = Dispatchers.Default,
) : CookiesStorage {
    private var loadedContainer: CookieContainer? = null
    private val mutex = Mutex()
    private val pendingExport = Channel<String>(Channel.CONFLATED)

    // Ends once [close] closed the channel and the last export has been written, so it needs no parent job
    private val writer = CoroutineScope(writeContext).launch {
        for (json in pendingExport) {
            try {
                dataStoreService.setPreference(key = Configuration.DATASTORE_KEY_COOKIES, value = json)
            } catch (e: Throwable) {
                errorService.emit(e)
            }
        }
    }

    suspend fun reset() = mutex.withLock {
        val container = container()
        container.cookies.clear()
        container.oldestCookie.value = 0L
        exportToDataStore(container)
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val container = container()
        val now = getTimeMillis()
        if (now >= container.oldestCookie.value) cleanup(container, now)

        return@withLock container.cookies.filter { it.matches(requestUrl) }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        with(cookie) {
            if (name.isBlank()) return@withLock
        }

        val container = container()
        container.cookies.removeAll { it.name == cookie.name && it.matches(requestUrl) }
        container.cookies.add(cookie.fillDefaults(requestUrl))
        cookie.expires?.timestamp?.let { expires ->
            if (container.oldestCookie.value > expires) {
                container.oldestCookie.value = expires
            }
        }
        exportToDataStore(container)
    }

    /** Closes the storage and waits until the last cookies have been persisted. */
    internal suspend fun closeAndAwaitWrites() {
        close()
        writer.join()
    }

    override fun close() {
        pendingExport.close()
    }

    private fun cleanup(container: CookieContainer, timestamp: Long) {
        val removed = container.cookies.removeAll { cookie ->
            val expires = cookie.expires?.timestamp ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = container.cookies.fold(Long.MAX_VALUE) { acc, cookie ->
            cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
        }

        container.oldestCookie.value = newOldest
        if (removed) exportToDataStore(container)
    }

    private fun exportToDataStore(container: CookieContainer) {
        try {
            val exportableCookies = container.cookies.toExportableCookieList()
            val export = ExportableCookieContainer(
                cookies = exportableCookies,
                oldestCookie = container.oldestCookie.value
            )
            pendingExport.trySend(joseCompliantSerializer.encodeToString(export))
        } catch (e: Throwable) {
            errorService.emit(e)
        }
    }

    private suspend fun container(): CookieContainer =
        loadedContainer ?: importFromDataStore().also { loadedContainer = it }

    private suspend fun importFromDataStore(): CookieContainer {
        try {
            val input = dataStoreService.getPreference(Configuration.DATASTORE_KEY_COOKIES).firstOrNull()
            if (input == null) {
                return CookieContainer(cookies = mutableListOf(), oldestCookie = atomic(0L))
            } else {
                val export: ExportableCookieContainer = joseCompliantSerializer.decodeFromString(input)
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
