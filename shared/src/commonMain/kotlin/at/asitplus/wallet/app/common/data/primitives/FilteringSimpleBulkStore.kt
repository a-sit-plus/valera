package at.asitplus.wallet.app.common.data.primitives

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FilteringSimpleBulkStore<Key : Any, Value : Any>(
    val simpleStore: SimpleBulkStore<Key, Value>,
    /**
     * If this returns false, act as if the value is not stored
     */
    val filter: (Pair<Key, Value>) -> Boolean = { true },
) : SimpleBulkStore<Key, Value> {
    private val mutex = Mutex()

    override suspend fun get(keys: Set<Key>): Map<Key, Value?> = mutex.withLock {
        getInsecure(keys)
    }

    private suspend fun getInsecure(keys: Set<Key>): Map<Key, Value?> = simpleStore.get(
        keys
    ).applyFilter()

    override suspend fun put(entries: Map<Key, Value>): Map<Key, Value?> = mutex.withLock {
        simpleStore.put(entries)
    }.applyFilter()

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
        simpleStore.put(resolvedValues.filter {
            it.value.first == null
        }.mapValues {
            it.value.second
        })

        resolvedValues.mapValues {
            it.value.second
        }
    }

    override suspend fun remove(keys: Collection<Key>): Map<Key, Value?> = mutex.withLock {
        simpleStore.remove(keys)
    }.applyFilter()

    override suspend fun keys(): Set<Key> = simpleStore.entries().keys

    override suspend fun entries(): Map<Key, Value> = simpleStore.entries().filter { (key, value) ->
        filter(key to value)
    }

    override suspend fun removeAllEntries() = simpleStore.removeAllEntries()

    private fun Map<Key, Value?>.applyFilter() = mapValues { (key, value) ->
        value?.let {
            if (filter(key to value)) {
                value
            } else {
                null
            }
        }
    }
}