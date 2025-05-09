package at.asitplus.wallet.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Does not support concurrent write access
 */
data class SimpleBootstrappingLockingWriteBulkStore<Key: Any, Value: Any>(
    val simpleStore: SimpleStore<Key, Value>,
    val mutex: Mutex = Mutex(),
): SimpleBulkStore<Key, Value> {
    override suspend fun get(keys: Set<Key>): Map<Key, Value?> = keys.associateWith {
        simpleStore.get(it)
    }

    override suspend fun put(entries: Map<Key, Value>): Map<Key, Value?> = mutex.withLock {
        entries.mapValues { (key, value) ->
            simpleStore.put(key, value)
        }
    }

    override suspend fun getOrPut(defaultValues: Map<Key, suspend () -> Value>): Map<Key, Value> = mutex.withLock {
        defaultValues.mapValues { (key, defaultValue) ->
            coroutineScope {
                async {
                    simpleStore.get(key) ?: run {
                        val value = defaultValue()
                        simpleStore.getOrPut(key) {
                            value
                        }
                    }
                }
            }
        }.also {
            it.values.joinAll()
        }.mapValues {
            it.value.await()
        }
    }

    override suspend fun remove(keys: Collection<Key>): Map<Key, Value?> = mutex.withLock {
        keys.associateWith {
            simpleStore.remove(it)
        }
    }

    override suspend fun keys(): Set<Key> = simpleStore.keys()

    override suspend fun entries() = simpleStore.entries()

    override suspend fun removeAllEntries() = mutex.withLock {
        simpleStore.removeAllEntries()
    }
}