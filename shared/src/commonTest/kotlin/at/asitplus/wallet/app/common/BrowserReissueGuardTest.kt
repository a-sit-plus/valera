package at.asitplus.wallet.app.common

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

class BrowserReissueGuardTest {
    private val timeSource = TestTimeSource()
    private val guard = BrowserReissueGuard(abandonedAfter = 5.minutes, timeSource = timeSource)

    @Test
    fun rejectsSecondCredentialWhileFirstIsInBrowser() = runTest {
        assertTrue(guard.tryAcquire(1))

        assertFalse(guard.tryAcquire(2))
    }

    @Test
    fun acceptsSameCredentialAgain() = runTest {
        assertTrue(guard.tryAcquire(1))

        assertTrue(guard.tryAcquire(1))
    }

    @Test
    fun acceptsNextCredentialOnceFirstIsFinished() = runTest {
        assertTrue(guard.tryAcquire(1))
        guard.release(1)

        assertTrue(guard.tryAcquire(2))
    }

    @Test
    fun keepsGuardWhenOtherCredentialIsReleased() = runTest {
        assertTrue(guard.tryAcquire(1))
        guard.release(2)

        assertFalse(guard.tryAcquire(2))
    }

    @Test
    fun acceptsNextCredentialOnceRequestsAreCleared() = runTest {
        assertTrue(guard.tryAcquire(1))
        guard.clear()

        assertTrue(guard.tryAcquire(2))
    }

    @Test
    fun takesAuthorizationAsAbandonedAfterTimeout() = runTest {
        assertTrue(guard.tryAcquire(1))
        timeSource += 4.minutes
        assertFalse(guard.tryAcquire(2))

        timeSource += 2.minutes
        assertTrue(guard.tryAcquire(2))
        assertFalse(guard.tryAcquire(1))
    }
}
