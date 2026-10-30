package data.storage

import at.asitplus.wallet.lib.utils.MapStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Hides removed entries at once, but removes them from [delegate] only on [flushRemovals], so that a caller can take
 * a value without waiting for a slow write, e.g. VC-K taking the PKCE code verifier right before the token request.
 */
class DeferredRemovalMapStore<Key : Any, Value : Any>(
    private val delegate: MapStore<Key, Value>,
) : MapStore<Key, Value> {
    private val mutex = Mutex()
    private val pendingRemovals = mutableSetOf<Key>()

    override suspend fun put(key: Key, value: Value) {
        mutex.withLock { pendingRemovals -= key }
        delegate.put(key, value)
    }

    override suspend fun get(key: Key): Value? =
        if (mutex.withLock { key in pendingRemovals }) null else delegate.get(key)

    override suspend fun remove(key: Key): Value? =
        if (mutex.withLock { pendingRemovals.add(key) }) delegate.get(key) else null

    /** Removes the entries taken by [remove] from [delegate]. */
    suspend fun flushRemovals() {
        mutex.withLock { pendingRemovals.toList() }.forEach { key ->
            delegate.remove(key)
            mutex.withLock { pendingRemovals -= key }
        }
    }
}
