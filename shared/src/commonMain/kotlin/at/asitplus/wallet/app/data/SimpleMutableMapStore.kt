package at.asitplus.wallet.app.data

data class SimpleMutableMapStore<Key : Any, Value : Any>(
    val mutableMap: MutableMap<Key, Value> = mutableMapOf(),
) : SimpleStore<Key, Value> {
    override suspend fun get(key: Key): Value? = mutableMap[key]

    override suspend fun put(key: Key, value: Value): Value? = mutableMap.put(key, value)

    override suspend fun getOrPut(key: Key, defaultValue: suspend () -> Value): Value =
        mutableMap[key] ?: defaultValue().also {
            mutableMap[key] = it
        }

    override suspend fun keys(): Set<Key> = mutableMap.keys


    override suspend fun entries(): Map<Key, Value> = mutableMap

    override suspend fun removeAllEntries() = mutableMap.clear()

    override suspend fun remove(key: Key): Value? = mutableMap.remove(key)
}