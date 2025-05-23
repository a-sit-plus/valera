package at.asitplus.wallet.app.common.data.primitives

import kotlin.jvm.JvmName

/**
 * Preserves concurrency properties of underlying store.
 */
data class TransformingSimpleBulkStore<Key : Any, Value : Any, InternalKey : Any, InternalValue : Any>(
    val simpleStore: SimpleBulkStore<InternalKey, InternalValue>,
    val keyMapping: Bijection<Key, InternalKey>,
    val valueMapping: Bijection<Value, InternalValue>,
) : SimpleBulkStore<Key, Value> {
    override suspend fun get(keys: Set<Key>): Map<Key, Value?> = simpleStore.get(
        keys.map {
            keyMapping.forwards(it)
        }.toSet()
    ).export()

    override suspend fun put(entries: Map<Key, Value>): Map<Key, Value?> = simpleStore.put(
        entries.mapKeys {
            keyMapping.forwards(it.key)
        }.mapValues {
            valueMapping.forwards(it.value)
        }
    ).export()

    override suspend fun getOrPut(defaultValues: Map<Key, suspend () -> Value>): Map<Key, Value> = simpleStore.getOrPut(
        defaultValues.mapKeys {
            keyMapping.forwards(it.key)
        }.mapValues {
            {
                valueMapping.forwards(it.value())
            }
        }
    ).export()

    override suspend fun remove(keys: Collection<Key>): Map<Key, Value?> = simpleStore.remove(
        keys.map {
            keyMapping.forwards(it)
        }.toSet()
    ).export()


    override suspend fun keys(): Set<Key> = simpleStore.keys().map {
        keyMapping.backwards(it)
    }.toSet()

    override suspend fun entries(): Map<Key, Value> = simpleStore.entries().export()

    override suspend fun removeAllEntries() = simpleStore.removeAllEntries()

    @JvmName("applyFilterWithoutNullValues")
    private fun Map<InternalKey, InternalValue>.export() = mapValues { entry ->
        entry.value.let {
            valueMapping.backwards(it)
        }
    }.mapKeys {
        keyMapping.backwards(it.key)
    }
    @JvmName("applyFilterWithNullValues")
    private fun Map<InternalKey, InternalValue?>.export() = mapValues { entry ->
        entry.value?.let {
            valueMapping.backwards(it)
        }
    }.mapKeys {
        keyMapping.backwards(it.key)
    }
}

