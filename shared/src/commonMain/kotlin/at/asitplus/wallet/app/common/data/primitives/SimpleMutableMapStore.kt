package at.asitplus.wallet.app.common.data.primitives

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SimpleMutableMapStore<Key : Any, Value : Any>(
    val mutableMap: MutableMap<Key, Value> = mutableMapOf(),
) : SimpleStore<Key, Value> {
    private val mutex: Mutex = Mutex()

    override suspend fun get(key: Key): Value? = mutex.withLock {
        mutableMap[key]
    }

    override suspend fun put(key: Key, value: Value): Value? = mutex.withLock {
        mutableMap.put(key, value)
    }

    override suspend fun getOrPut(key: Key, defaultValue: suspend () -> Value): Value = mutex.withLock {
        mutableMap[key] ?: defaultValue().also {
            mutableMap[key] = it
        }
    }

    override suspend fun keys(): Set<Key> = mutex.withLock {
        mutableMap.keys
    }


    override suspend fun entries(): Map<Key, Value> = mutableMap

    override suspend fun removeAllEntries() = mutex.withLock {
        mutableMap.clear()
    }

    override suspend fun remove(key: Key): Value? = mutex.withLock {
        mutableMap.remove(key)
    }
}