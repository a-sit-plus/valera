package at.asitplus.wallet.app.common.data.primitives

interface SimpleBulkStore<Key : Any, Value : Any> : SimpleStore<Key, Value> {
    /**
     * implementation must ensure that each key in keys is in the resulting map
     */
    suspend fun get(keys: Set<Key>): Map<Key, Value?>
    override suspend fun get(key: Key): Value? = get(setOf(key)).runCatching {
        getValue(key)
    }.getOrElse {
        throw SimpleBulkStoreImplementationException(it)
    }

    /**
     * implementation must ensure that each key in entries is in the resulting map
     */
    suspend fun put(entries: Map<Key, Value>): Map<Key, Value?>
    override suspend fun put(key: Key, value: Value): Value? = put(mapOf(key to value)).runCatching {
        getValue(key)
    }.getOrElse {
        throw SimpleBulkStoreImplementationException(it)
    }

    /**
     * implementation must ensure that each key in defaultValues is in the resulting map
     */
    suspend fun getOrPut(defaultValues: Map<Key, suspend () -> Value>): Map<Key, Value>
    override suspend fun getOrPut(key: Key, defaultValue: suspend () -> Value): Value = getOrPut(
        mapOf(key to defaultValue),
    ).runCatching {
        getValue(key)
    }.getOrElse {
        throw SimpleBulkStoreImplementationException(it)
    }

    /**
     * implementation must ensure that each key in keys is in the resulting map
     */
    suspend fun remove(keys: Collection<Key>): Map<Key, Value?>
    override suspend fun remove(key: Key): Value? = remove(listOf(key)).runCatching {
        getValue(key)
    }.getOrElse {
        throw SimpleBulkStoreImplementationException(it)
    }
}