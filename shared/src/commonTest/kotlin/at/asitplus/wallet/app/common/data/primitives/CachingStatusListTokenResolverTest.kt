package at.asitplus.wallet.app.common.data.primitives

import at.asitplus.wallet.lib.agent.StatusListAgent
import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CachingStatusListTokenResolverTest {
    private val url = UniformResourceIdentifier("https://example.test/status")

    private class MapStore : SimpleStore<UniformResourceIdentifier, StatusListToken> {
        val map = mutableMapOf<UniformResourceIdentifier, StatusListToken>()
        override suspend fun get(key: UniformResourceIdentifier) = map[key]
        override suspend fun set(key: UniformResourceIdentifier, value: StatusListToken) = map.put(key, value)
        override suspend fun getOrPut(
            key: UniformResourceIdentifier,
            defaultValue: suspend () -> StatusListToken,
        ) = map[key] ?: defaultValue().also { map[key] = it }

        override suspend fun keys() = map.keys.toSet()
        override suspend fun entries() = map.toMap()
        override suspend fun remove(key: UniformResourceIdentifier) = map.remove(key)
        override suspend fun removeAllEntries() = map.clear()
    }

    private suspend fun token() = StatusListJwt(StatusListAgent().issueStatusListJwt(), resolvedAt = null)

    @Test
    fun reusesTheCachedToken() = runTest {
        val cached = token()
        val store = MapStore().apply { map[url] = cached }
        var resolved = 0
        val resolver = CachingStatusListTokenResolver(store, StatusListTokenResolver { resolved++; token() })

        assertSame(cached, resolver(url))
        assertEquals(0, resolved)
    }

    @Test
    fun freshResolvesAgainAndReplacesTheCachedToken() = runTest {
        val store = MapStore().apply { map[url] = token() }
        val fetched = token()
        val resolver = CachingStatusListTokenResolver(
            store = store,
            statusListTokenResolver = { error("The cached resolver must not be used") },
            freshStatusListTokenResolver = { fetched },
        )

        assertSame(fetched, resolver.fresh(url))
        assertSame(fetched, store.map[url])
        assertSame(fetched, resolver(url))
    }

    @Test
    fun freshDropsTheCachedTokenIfResolvingFails() = runTest {
        val store = MapStore().apply { map[url] = token() }
        val resolver = CachingStatusListTokenResolver(store, StatusListTokenResolver { error("offline") })

        assertFailsWith<IllegalStateException> { resolver.fresh(url) }
        assertNull(store.map[url])
    }

    @Test
    fun cachedReturnsTheCachedTokenWithoutResolving() = runTest {
        val cached = token()
        val store = MapStore().apply { map[url] = cached }
        val resolver = CachingStatusListTokenResolver(store, StatusListTokenResolver { error("Must not resolve") })

        assertSame(cached, resolver.cached(url))
    }

    @Test
    fun cachedFailsWithoutACachedTokenInsteadOfResolvingIt() = runTest {
        val store = MapStore()
        val resolver = CachingStatusListTokenResolver(store, StatusListTokenResolver { error("Must not resolve") })

        assertFailsWith<NoSuchElementException> { resolver.cached(url) }
        assertNull(store.map[url])
    }

    @Test
    fun resolvesAListOnceForConcurrentLookups() = runTest {
        val store = MapStore()
        val fetched = token()
        val release = CompletableDeferred<Unit>()
        var resolved = 0
        val resolver = CachingStatusListTokenResolver(store, StatusListTokenResolver {
            resolved++
            release.await()
            fetched
        })

        val lookups = List(3) { async { resolver(url) } }
        testScheduler.runCurrent()
        release.complete(Unit)

        lookups.awaitAll().forEach { assertSame(fetched, it) }
        assertEquals(1, resolved)
    }

    @Test
    fun cachedTokensAndOtherListsDoNotWaitForAResolution() = runTest {
        val otherUrl = UniformResourceIdentifier("https://example.test/other")
        val cachedUrl = UniformResourceIdentifier("https://example.test/cached")
        val cached = token()
        val store = MapStore().apply { map[cachedUrl] = cached }
        val other = token()
        val release = CompletableDeferred<Unit>()
        val resolver = CachingStatusListTokenResolver(store, StatusListTokenResolver {
            if (it == url) release.await()
            other
        })

        val pending = async { resolver(url) }
        testScheduler.runCurrent()

        assertSame(cached, resolver(cachedUrl))
        assertSame(other, resolver(otherUrl))
        assertTrue(pending.isActive)
        release.complete(Unit)
        pending.await()
    }
}
