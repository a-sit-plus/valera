package at.asitplus.wallet.app.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
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
    fun requestIgnoringTheCooldownIsSent() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("nope", HttpStatusCode.ServiceUnavailable) }) {
            install(HttpFailureCache) { cooldown = 1.hours }
        }

        assertEquals("nope", client.get(url).bodyAsText())
        assertFailsWith<HttpResourceInCooldownException> { client.get(url) }

        val response = client.get(url) { attributes.put(IgnoreFailureCooldown, Unit) }
        assertEquals("nope", response.bodyAsText())
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

    @Test
    fun requestWithoutResponseCoolsDownForTheTransientCooldown() = runTest {
        var requests = 0
        val clock = MutableClock(Instant.fromEpochMilliseconds(0))
        val client = HttpClient(MockEngine { requests++; throw IOException("offline") }) {
            install(HttpFailureCache) {
                cooldown = 1.hours
                transientCooldown = 5.minutes
                this.clock = clock
            }
        }

        assertFailsWith<IOException> { client.get(url) }
        assertFailsWith<HttpResourceInCooldownException> { client.get(url) }
        assertEquals(1, requests)

        clock.instant = Instant.fromEpochMilliseconds(6.minutes.inWholeMilliseconds)
        assertFailsWith<IOException> { client.get(url) }
        assertEquals(2, requests)
    }

    @Test
    fun unusableResponseCoolsDownWithoutShorteningAnErrorCooldown() = runTest {
        var requests = 0
        var status = HttpStatusCode.OK
        val clock = MutableClock(Instant.fromEpochMilliseconds(0))
        val client = HttpClient(MockEngine { requests++; respond("garbage", status) }) {
            install(HttpFailureCache) {
                cooldown = 1.hours
                transientCooldown = 5.minutes
                this.clock = clock
            }
        }

        assertFailsWith<IllegalStateException> {
            client.get(url).coolingDownIfUnusable { error("Invalid document: ${bodyAsText()}") }
        }
        assertFailsWith<HttpResourceInCooldownException> { client.get(url) }
        assertEquals(1, requests)

        // An error status cools down for longer, which reporting it as unusable as well keeps
        clock.instant = Instant.fromEpochMilliseconds(6.minutes.inWholeMilliseconds)
        status = HttpStatusCode.ServiceUnavailable
        assertFailsWith<IllegalArgumentException> {
            client.get(url).coolingDownIfUnusable { require(this.status.value < 400) }
        }
        clock.instant = Instant.fromEpochMilliseconds(30.minutes.inWholeMilliseconds)
        assertFailsWith<HttpResourceInCooldownException> { client.get(url) }
        assertEquals(2, requests)
    }

    @Test
    fun usableResponseIsNotCooledDown() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("ok", HttpStatusCode.OK) }) {
            install(HttpFailureCache) { transientCooldown = 5.minutes }
        }

        assertEquals("ok", client.get(url).coolingDownIfUnusable { bodyAsText() })
        assertEquals("ok", client.get(url).coolingDownIfUnusable { bodyAsText() })
        assertEquals(2, requests)
    }
}
