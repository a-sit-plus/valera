package at.asitplus.wallet.app.common.data.primitives

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SimpleBootstrappingBulkStore<Key : Any, Value : Any>(
    val simpleStore: SimpleStore<Key, Value>,
) : SimpleBulkStore<Key, Value> {
    private val mutex: Mutex = Mutex()

    override suspend fun get(keys: Set<Key>): Map<Key, Value?> = mutex.withLock {
        keys.associateWith {
            simpleStore.get(it)
        }
    }

    private suspend fun getInsecure(keys: Set<Key>): Map<Key, Value?> = keys.associateWith {
        simpleStore.get(it)
    }

    override suspend fun put(entries: Map<Key, Value>): Map<Key, Value?> = mutex.withLock {
        putInsecure(entries)
    }

    private suspend fun putInsecure(entries: Map<Key, Value>): Map<Key, Value?> = entries.mapValues { (key, value) ->
        simpleStore.put(key, value)
    }

    override suspend fun getOrPut(defaultValues: Map<Key, suspend () -> Value>): Map<Key, Value> = mutex.withLock {
        val available = getInsecure(defaultValues.keys)

        // resolve all necessary default values
        val pendingValues = defaultValues.mapValues { (key, default) ->
            val value = runCatching {
                available.getValue(key)
            }.getOrElse {
                throw SimpleBulkStoreImplementationException(it)
            }

            value to coroutineScope {
                async {
                    value ?: default()
                }
            }
        }

        // wait for resolving default values and store them in the store
        pendingValues.values.map {
            it.second
        }.joinAll()
        val resolvedValues = pendingValues.mapValues {
            it.value.first to it.value.second.await()
        }
        putInsecure(resolvedValues.filter {
            it.value.first == null
        }.mapValues {
            it.value.second
        })

        resolvedValues.mapValues {
            it.value.second
        }
    }

    override suspend fun remove(keys: Collection<Key>): Map<Key, Value?> = mutex.withLock {
        keys.associateWith {
            simpleStore.remove(it)
        }
    }

    override suspend fun keys(): Set<Key> = mutex.withLock {
        simpleStore.keys()
    }

    override suspend fun entries() = mutex.withLock {
        simpleStore.entries()
    }

    override suspend fun removeAllEntries() = mutex.withLock {
        simpleStore.removeAllEntries()
    }
}