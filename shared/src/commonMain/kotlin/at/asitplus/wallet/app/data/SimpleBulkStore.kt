package at.asitplus.wallet.app.data

interface SimpleBulkStore<Key : Any, Value : Any>: SimpleStore<Key, Value> {
    /**
     * @requires each key in keys must be in the resulting map
     */
    suspend fun get(keys: Set<Key>): Map<Key, Value?>
    override suspend fun get(key: Key): Value? = get(setOf(key)).getOrElse(key) {
        throw SimpleBulkStoreImplementationException()
    }

    /**
     * @requires each key in entries.keys must be in the resulting map
     */
    suspend fun put(entries: Map<Key, Value>): Map<Key, Value?>
    override suspend fun put(key: Key, value: Value): Value? = put(mapOf(key to value)).getOrElse(key) {
        throw SimpleBulkStoreImplementationException()
    }

    /**
     * @requires each key in defaultValues.keys must be in the resulting map
     */
    suspend fun getOrPut(defaultValues: Map<Key, suspend () -> Value>): Map<Key, Value>
    override suspend fun getOrPut(key: Key, defaultValue: suspend () -> Value): Value = getOrPut(mapOf(key to defaultValue)).getOrElse(key) {
        throw SimpleBulkStoreImplementationException()
    }

    /**
     * @requires each key in keys must be in the resulting map
     */
    suspend fun remove(keys: Collection<Key>): Map<Key, Value?>
    override suspend fun remove(key: Key): Value? = remove(listOf(key)).getOrElse(key) {
        throw SimpleBulkStoreImplementationException()
    }
}
