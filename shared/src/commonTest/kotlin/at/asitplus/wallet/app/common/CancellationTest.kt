package at.asitplus.wallet.app.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class CancellationTest {

    @Test
    fun cancellationOfAnotherJobIsAFailure() = runTest {
        // E.g. a shared refresh, cancelled by the session that started it
        val refresh = CompletableDeferred<Boolean>().apply { cancel() }

        val result = catchingUnlessCancelled { refresh.await() }

        assertIs<CancellationException>(result.exceptionOrNull()?.cause)
    }

    @Test
    fun cancellationOfTheCallerIsRethrown() = runTest {
        var completed = false
        val caller = launch {
            catchingUnlessCancelled {
                cancel()
                throw CancellationException("Left the flow")
            }
            completed = true
        }
        caller.join()

        assertTrue(caller.isCancelled)
        assertFalse(completed)
    }

    @Test
    fun failureCooldownIsAFailure() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }) {
            install(HttpFailureCache) { cooldown = 1.hours }
        }
        client.get(URL)

        val result = catchingUnlessCancelled { client.get(URL) }

        assertIs<HttpResourceInCooldownException>(result.exceptionOrNull())
    }

    @Test
    fun requestOfAClosedClientIsAFailure() = runTest {
        val client = HttpClient(MockEngine { respond("") }).apply { close() }

        val result = catchingUnlessCancelled { client.get(URL) }

        assertTrue(result.isFailure)
    }

    @Test
    fun foreignCancellationFailsInsteadOfCancellingTheCaller() = runTest {
        val result = runCatching { failOnForeignCancellation { throw CancellationException("Request cancelled") } }

        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Request cancelled", result.exceptionOrNull()?.cause?.message)
    }

    private companion object {
        const val URL = "https://example.test/trust-list"
    }
}
