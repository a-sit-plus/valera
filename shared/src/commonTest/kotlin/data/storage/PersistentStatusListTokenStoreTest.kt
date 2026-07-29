package data.storage

import at.asitplus.wallet.app.common.data.primitives.CacheStoreEntry
import at.asitplus.wallet.app.common.data.primitives.SimpleBootstrappingBulkStore
import at.asitplus.wallet.app.common.data.primitives.SimpleCacheStoreWrapper
import at.asitplus.wallet.lib.agent.FixedTimeClock
import at.asitplus.wallet.lib.agent.StatusListAgent
import at.asitplus.wallet.lib.data.StatusListCwt
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PersistentStatusListTokenStoreTest {
    @Test
    fun persistsTokenAndItsCacheTime() = runTest {
        val dataStore = DummyDataStoreService()
        val url = UniformResourceIdentifier("https://example.test/status")
        val cachedAt = Instant.fromEpochMilliseconds(1_000_000)
        val token = StatusListJwt(
            value = StatusListAgent().issueStatusListJwt(),
            resolvedAt = cachedAt,
        )

        cache(dataStore, FixedTimeClock(cachedAt.toEpochMilliseconds())).set(url, token)

        assertEquals(
            token,
            cache(dataStore, FixedTimeClock((cachedAt + 4.minutes).toEpochMilliseconds()))[url],
        )
        assertNull(
            cache(dataStore, FixedTimeClock((cachedAt + 6.minutes).toEpochMilliseconds()))[url],
        )
    }

    @Test
    fun skipsNonJwtTokensWithoutThrowing() = runTest {
        val dataStore = DummyDataStoreService()
        val url = UniformResourceIdentifier("https://example.test/status-cwt")
        val cachedAt = Instant.fromEpochMilliseconds(1_000_000)
        val cwt: CacheStoreEntry<StatusListToken> = CacheStoreEntry(
            data = StatusListCwt(value = StatusListAgent().issueStatusListCwt(), resolvedAt = cachedAt),
            createdTime = cachedAt,
        )
        val store = PersistentStatusListTokenStore(dataStore)

        assertNull(store.set(url, cwt))
        assertNull(store[url])
    }

    @Test
    fun boundsPersistedTokens() = runTest {
        val dataStore = DummyDataStoreService()
        val cachedAt = Instant.fromEpochMilliseconds(1_000_000)
        val entry: CacheStoreEntry<StatusListToken> = CacheStoreEntry(
            data = StatusListJwt(StatusListAgent().issueStatusListJwt(), resolvedAt = cachedAt),
            createdTime = cachedAt,
        )
        val store = PersistentStatusListTokenStore(dataStore)

        repeat(65) {
            store[UniformResourceIdentifier("https://example.test/status/$it")] = entry
        }

        assertEquals(64, store.keys().size)
        assertFalse(UniformResourceIdentifier("https://example.test/status/0") in store.keys())
    }

    private fun cache(dataStore: DataStoreService, clock: FixedTimeClock) = SimpleCacheStoreWrapper(
        store = SimpleBootstrappingBulkStore(PersistentStatusListTokenStore(dataStore)),
        clock = clock,
        getCachingDuration = { 5.minutes },
        onEntryFiltered = {},
    )
}
