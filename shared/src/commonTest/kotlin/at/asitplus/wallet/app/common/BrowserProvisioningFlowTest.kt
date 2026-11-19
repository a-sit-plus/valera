package at.asitplus.wallet.app.common

import data.storage.DummyDataStoreService
import data.storage.persistentStringMapStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class BrowserProvisioningFlowTest {
    private var now = Instant.fromEpochMilliseconds(0)
    private val clock = object : Clock {
        override fun now() = now
    }
    private val dataStore = DummyDataStoreService()
    private val flow = newSession()

    @Test
    fun newIssuanceAndReissuePreserveTheFirstCallbackInEitherOrder() = runTest {
        for ((first, second) in listOf("issue" to "reissue", "reissue" to "issue")) {
            start(flow, first)

            assertFailsWith<BrowserProvisioningInProgressException> {
                start(newSession(), second, prepare = { fail("A blocked flow must not send an authorization request") })
            }
            assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), first) }
            assertFailsWith<IllegalArgumentException> {
                newSession().resume(second) { _, _ -> fail("Wrong callback state") }
            }

            newSession().resume(first) { context, attestation ->
                assertEquals("context-$first", context)
                assertEquals("attestation-$first", attestation)
                assertEquals("verifier-$first", store("verifiers").get(first))
            }
            assertNull(store("verifiers").get(first))
        }
    }

    @Test
    fun blocksOtherSessionsDuringAuthorizationSetup() = runTest {
        val preparing = CompletableDeferred<Unit>()
        val continueStart = CompletableDeferred<Unit>()
        val job = launch {
            start(flow, "first", prepare = {
                preparing.complete(Unit)
                continueStart.await()
            })
        }
        preparing.await()

        assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), "second") }

        continueStart.complete(Unit)
        job.join()
        newSession().resume("first") { _, _ -> }
    }

    @Test
    fun callbackFailureReleasesSlotAndClearsContext() = runTest {
        start(flow, "first")
        val error = assertFailsWith<IllegalStateException> {
            newSession().resume("first") { _, _ ->
                assertFailsWith<BrowserProvisioningInProgressException> { start(flow, "second") }
                error("Token exchange failed")
            }
        }

        assertEquals("Token exchange failed", error.message)
        assertFalse(flow.isActive())
        assertNull(store("contexts").get("first"))
        assertNull(store("attestations").get("first"))
        assertNull(store("verifiers").get("first"))
        assertFailsWith<IllegalStateException> {
            flow.resume("first") { _, _ -> fail("Callback was already consumed") }
        }
        start(flow, "second")
    }

    @Test
    fun browserLaunchFailureReleasesSlotAndClearsContext() = runTest {
        assertFailsWith<IllegalStateException> {
            start(flow, "first", open = { error("Cannot open browser") })
        }

        assertFalse(flow.isActive())
        assertNull(store("contexts").get("first"))
        assertNull(store("attestations").get("first"))
        assertNull(store("verifiers").get("first"))
        start(newSession(), "second")
    }

    @Test
    fun abandonedLoginExpiresAcrossSessions() = runTest {
        start(flow, "old")
        now += 9.minutes
        assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), "new") }

        now += 2.minutes
        start(newSession(), "new")
        assertFailsWith<IllegalArgumentException> {
            flow.resume("old") { _, _ -> fail("Expired authorization was resumed") }
        }
        flow.resume("new") { context, _ -> assertEquals("context-new", context) }
    }

    @Test
    fun acceptsLegacyPersistedContextWithoutAnActiveMarker() = runTest {
        store("contexts").put("legacy", "context-legacy")

        newSession().resume("legacy") { context, _ -> assertEquals("context-legacy", context) }

        assertNull(store("contexts").get("legacy"))
        assertFalse(flow.isActive())
    }

    @Test
    fun cancellationDuringCallbackReleasesTheSlot() = runTest {
        start(flow, "first")
        val callbackStarted = CompletableDeferred<Unit>()
        val callback = launch {
            newSession().resume("first") { _, _ ->
                callbackStarted.complete(Unit)
                awaitCancellation()
            }
        }
        callbackStarted.await()
        callback.cancelAndJoin()

        assertFalse(flow.isActive())
        assertNull(store("verifiers").get("first"))
        start(flow, "second")
    }

    @Test
    fun lateCallbackCleanupDoesNotRemoveANewerLogin() = runTest {
        start(flow, "old")
        newSession().resume("old") { _, _ ->
            now += 11.minutes
            start(flow, "new")
        }

        assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), "third") }
        flow.resume("new") { context, _ -> assertEquals("context-new", context) }
    }

    @Test
    fun replacesOnlyThePendingLoginAndClearsItsData() = runTest {
        start(flow, "old")
        val conflict = assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), "new") }

        val abandoned = newSession().abandon(requireNotNull(conflict.pendingState)) {
            assertEquals("context-old", it)
        }
        assertTrue(abandoned)
        start(flow, "new")

        assertNull(store("contexts").get("old"))
        assertNull(store("attestations").get("old"))
        assertNull(store("verifiers").get("old"))
        assertFailsWith<IllegalArgumentException> {
            newSession().resume("old") { _, _ -> fail("Replaced login was resumed") }
        }
        flow.resume("new") { context, _ -> assertEquals("context-new", context) }
    }

    @Test
    fun staleReplacementCannotAbandonALoginOnceItsCallbackStarts() = runTest {
        start(flow, "first")
        val conflict = assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), "next") }

        newSession().resume("first") { _, _ ->
            val abandoned = flow.abandon(requireNotNull(conflict.pendingState)) {
                fail("Abandoned a callback in progress")
            }
            assertFalse(abandoned)
            val busy = assertFailsWith<BrowserProvisioningInProgressException> { start(flow, "next") }
            assertNull(busy.pendingState)
        }
    }

    @Test
    fun staleReplacementCannotAbandonANewerLogin() = runTest {
        start(flow, "old")
        val conflict = assertFailsWith<BrowserProvisioningInProgressException> { start(newSession(), "next") }
        flow.resume("old") { _, _ -> }
        start(flow, "new")

        val abandoned = newSession().abandon(requireNotNull(conflict.pendingState)) {
            fail("Abandoned a newer login")
        }

        assertFalse(abandoned)
        flow.resume("new") { context, _ -> assertEquals("context-new", context) }
    }

    private fun store(key: String) = persistentStringMapStore(dataStore, key, clock = clock)

    private fun newSession() = BrowserProvisioningFlow(
        activeStates = store("active"),
        contexts = store("contexts"),
        attestations = store("attestations"),
        codeVerifiers = store("verifiers"),
    )

    private suspend fun start(
        flow: BrowserProvisioningFlow,
        state: String,
        prepare: suspend () -> Unit = {},
        open: (String) -> Unit = {},
    ) = flow.start(
        prepare = {
            prepare()
            store("verifiers").put(state, "verifier-$state")
            state
        },
        stateOf = { it },
        persist = {
            store("contexts").put(it, "context-$it")
            store("attestations").put(it, "attestation-$it")
        },
        open = open,
    )
}
