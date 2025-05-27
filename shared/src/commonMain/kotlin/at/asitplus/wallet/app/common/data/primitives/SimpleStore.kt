package at.asitplus.wallet.app.common.data.primitives

interface SimpleStore<Key : Any, Value : Any> {
    suspend fun get(key: Key): Value?

    suspend fun put(key: Key, value: Value): Value?

    suspend fun getOrPut(key: Key, defaultValue: suspend () -> Value): Value

    suspend fun keys(): Set<Key>

    suspend fun entries(): Map<Key, Value>

    suspend fun remove(key: Key): Value?

    suspend fun removeAllEntries()
}

