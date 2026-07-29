package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class HttpFailureCacheConfig {
    var cooldown: Duration = Duration.ZERO
    var clock: Clock = Clock.System
}

/** Thrown (instead of making a request) while a URL is cooling down after a recent error response. */
class HttpResourceInCooldownException(url: String, until: Instant) :
    Exception("$url is in failure cooldown until $until (last response was an error)")

/**
 * Client plugin that stops re-fetching a GET URL that just returned an error status. After a `>= 400` response the
 * URL is put on a [HttpFailureCacheConfig.cooldown]; while cooling, further GETs to it fail immediately without a
 * request, and a successful response clears it. This complements [io.ktor.client.plugins.cache.HttpCache] (which
 * only caches `2xx`): together they mean a broken remote resource (404/503) is hit at most once per cooldown
 * instead of on every freshness check / UI visit.
 *
 * Network errors (offline, DNS, timeout) do NOT trigger a cooldown — no response reaches [onResponse], and a
 * transient offline moment shouldn't penalize a URL. State is in-memory per client, so it resets on app restart.
 * Note: this keys on HTTP status only, so a `200` with an unparseable body is not cooled down.
 */
val HttpFailureCache = createClientPlugin("HttpFailureCache", ::HttpFailureCacheConfig) {
    val cooldown = pluginConfig.cooldown
    val clock = pluginConfig.clock
    val mutex = Mutex()
    val coolingUntil = mutableMapOf<String, Instant>()

    onRequest { request, _ ->
        if (request.method == HttpMethod.Get) {
            val url = request.url.build().toString()
            mutex.withLock {
                coolingUntil[url]?.let { until ->
                    if (clock.now() < until) {
                        // Silent on purpose: logging here would reproduce the request flood we are avoiding.
                        throw HttpResourceInCooldownException(url, until)
                    }
                    coolingUntil.remove(url)
                }
            }
        }
    }

    onResponse { response ->
        if (response.request.method == HttpMethod.Get) {
            val url = response.request.url.toString()
            if (response.status.value >= 400) {
                val until = clock.now() + cooldown
                mutex.withLock { coolingUntil[url] = until }
                Napier.d("HTTP ${response.status.value} for $url; suppressing retries until $until")
            } else {
                mutex.withLock { coolingUntil.remove(url) }
            }
        }
    }
}
