package at.asitplus.wallet.app.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HttpFailureCacheTest {
    private class MutableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val url = "https://example.test/resource"

    @Test
    fun skipsRequestWhileCoolingDownThenRetries() = runTest {
        var requests = 0
        val clock = MutableClock(Instant.fromEpochMilliseconds(0))
        val client = HttpClient(MockEngine { requests++; respond("nope", HttpStatusCode.ServiceUnavailable) }) {
            install(HttpFailureCache) {
                cooldown = 1.hours
                this.clock = clock
            }
        }

        // First attempt reaches the server and gets 503; cooldown is recorded.
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get(url).status)
        assertEquals(1, requests)

        // Within the cooldown: fails immediately, no request made.
        assertFailsWith<HttpResourceInCooldownException> { client.get(url) }
        assertEquals(1, requests)

        // After the cooldown elapses: retries.
        clock.instant = Instant.fromEpochMilliseconds(61.minutes.inWholeMilliseconds)
        client.get(url)
        assertEquals(2, requests)
    }

    @Test
    fun successfulResponsesAreNeverSuppressed() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("ok", HttpStatusCode.OK) }) {
            install(HttpFailureCache) { cooldown = 1.hours }
        }

        assertEquals("ok", client.get(url).bodyAsText())
        assertEquals("ok", client.get(url).bodyAsText())
        assertEquals(2, requests)
    }
}
