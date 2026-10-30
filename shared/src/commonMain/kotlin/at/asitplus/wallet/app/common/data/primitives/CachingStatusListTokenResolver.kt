package at.asitplus.wallet.app.common.data.primitives

import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This class does not care about the underlying caching rules.
 */
data class CachingStatusListTokenResolver(
    val store: SimpleStore<UniformResourceIdentifier, StatusListToken>,
    val statusListTokenResolver: StatusListTokenResolver,
    /** Resolves a status list token for [fresh], bypassing every cache, by default [statusListTokenResolver]. */
    val freshStatusListTokenResolver: StatusListTokenResolver = statusListTokenResolver,
) : StatusListTokenResolver {
    // One resolution per list at a time, so that credentials sharing a list, checked at once, request it once, while
    // lookups of other lists and of cached tokens do not wait for the network, as with the lock of the store
    private val resolutionLocksGuard = SynchronizedObject()
    private val resolutionLocks = mutableMapOf<UniformResourceIdentifier, Mutex>()

    private fun resolutionLock(statusListUrl: UniformResourceIdentifier) =
        synchronized(resolutionLocksGuard) { resolutionLocks.getOrPut(statusListUrl) { Mutex() } }

    override suspend fun invoke(statusListUrl: UniformResourceIdentifier) =
        store[statusListUrl] ?: resolutionLock(statusListUrl).withLock {
            // A resolution of the same list, which this one waited for, may have cached it meanwhile
            store[statusListUrl] ?: statusListTokenResolver(statusListUrl).also { store[statusListUrl] = it }
        }

    /**
     * Always resolves the status list token again with [freshStatusListTokenResolver], e.g. for the status of a
     * credential just received, and caches it in place of the cached one. The cached token is dropped before, so that
     * it is not used any more even if resolving fails.
     */
    val fresh = StatusListTokenResolver { statusListUrl ->
        store.remove(statusListUrl)
        resolutionLock(statusListUrl).withLock {
            freshStatusListTokenResolver(statusListUrl).also { store[statusListUrl] = it }
        }
    }

    /**
     * Only returns a cached status list token that is still fresh, and fails for any other, without resolving it,
     * e.g. to order credentials without waiting for the network: VC-K reports a status that can not be resolved as
     * not checked.
     */
    val cached = StatusListTokenResolver { statusListUrl ->
        store[statusListUrl] ?: throw NoSuchElementException("No fresh cached status list token for $statusListUrl")
    }
}
