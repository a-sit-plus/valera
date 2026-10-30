package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class HttpFailureCacheConfig {
    /** Cooldown after an error status. */
    var cooldown: Duration = Duration.ZERO

    /**
     * Cooldown after a request that got no response, e.g. offline, unresolvable, or timed out, or whose response was
     * unusable, see [coolingDownIfUnusable].
     */
    var transientCooldown: Duration = Duration.ZERO
    var clock: Clock = Clock.System
}

/** Thrown (instead of making a request) while a URL is cooling down after a recent error response. */
class HttpResourceInCooldownException(url: String, until: Instant) :
    Exception("$url is in failure cooldown until $until (last request failed)")

/**
 * Marks a request that is sent even while its URL is cooling down, e.g. as its response must be fresh. Its response
 * still starts or clears the cooldown, see [HttpFailureCache].
 */
val IgnoreFailureCooldown = AttributeKey<Unit>("IgnoreFailureCooldown")

private class FailureCooldowns(private val clock: Clock) {
    private val mutex = Mutex()
    private val coolingUntil = mutableMapOf<String, Instant>()

    suspend fun check(url: String) = mutex.withLock {
        coolingUntil[url]?.let { until ->
            if (clock.now() < until) {
                // Silent on purpose: logging here would reproduce the request flood we are avoiding.
                throw HttpResourceInCooldownException(url, until)
            }
            coolingUntil.remove(url)
        }
    }

    /** Starts a cooldown of [url], keeping a longer one already running, e.g. after an error status. */
    suspend fun coolDown(url: String, cooldown: Duration, reason: String) {
        if (cooldown <= Duration.ZERO) return
        val until = mutex.withLock {
            maxOf(clock.now() + cooldown, coolingUntil[url] ?: Instant.DISTANT_PAST).also { coolingUntil[url] = it }
        }
        Napier.d("$reason for $url; suppressing retries until $until")
    }

    suspend fun clear(url: String) = mutex.withLock { coolingUntil.remove(url) }
}

private val FailureCooldownsKey = AttributeKey<Pair<FailureCooldowns, Duration>>("HttpFailureCooldowns")

/**
 * Client plugin that stops re-fetching a GET URL whose last request failed. After a `>= 400` response the URL is put
 * on a [HttpFailureCacheConfig.cooldown], after a request without a response, e.g. offline or timed out, or with an
 * unusable response, see [coolingDownIfUnusable], on the shorter [HttpFailureCacheConfig.transientCooldown]. While
 * cooling, further GETs to it fail immediately without a request, and a successful response clears it. This
 * complements [io.ktor.client.plugins.cache.HttpCache] (which only caches `2xx`): together they mean a broken or
 * unreachable remote resource is hit at most once per cooldown instead of on every freshness check / UI visit, each
 * of which would wait for the request to fail otherwise.
 *
 * State is in-memory per client, so it resets on app restart.
 */
val HttpFailureCache = createClientPlugin("HttpFailureCache", ::HttpFailureCacheConfig) {
    val cooldown = pluginConfig.cooldown
    val transientCooldown = pluginConfig.transientCooldown
    val cooldowns = FailureCooldowns(pluginConfig.clock)
    client.attributes.put(FailureCooldownsKey, cooldowns to transientCooldown)

    onRequest { request, _ ->
        if (request.method == HttpMethod.Get && !request.attributes.contains(IgnoreFailureCooldown)) {
            cooldowns.check(request.url.build().toString())
        }
    }

    on(Send) { request ->
        try {
            proceed(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (request.method == HttpMethod.Get) {
                cooldowns.coolDown(request.url.build().toString(), transientCooldown, "No response (${e.message})")
            }
            throw e
        }
    }

    onResponse { response ->
        if (response.request.method == HttpMethod.Get) {
            val url = response.request.url.toString()
            if (response.status.value >= 400) {
                cooldowns.coolDown(url, cooldown, "HTTP ${response.status.value}")
            } else {
                cooldowns.clear(url)
            }
        }
    }
}

/**
 * Runs [block], which reads and checks this response, and puts its URL on the transient cooldown of the
 * [HttpFailureCache] of its client if it fails, e.g. as a `200` body is no valid document, which would be requested
 * again on every check otherwise. A longer cooldown, e.g. of an error status, is kept.
 */
suspend inline fun <T> HttpResponse.coolingDownIfUnusable(block: HttpResponse.() -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    reportUnusable(e)
    throw e
}

@PublishedApi
internal suspend fun HttpResponse.reportUnusable(cause: Throwable) {
    if (request.method != HttpMethod.Get) return
    val (cooldowns, transientCooldown) = call.client.attributes.getOrNull(FailureCooldownsKey) ?: return
    cooldowns.coolDown(request.url.toString(), transientCooldown, "Unusable response (${cause.message})")
}
