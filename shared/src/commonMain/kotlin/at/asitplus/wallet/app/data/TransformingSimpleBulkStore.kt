package at.asitplus.wallet.app.data

data class TransformingSimpleBulkStore<Key : Any, Value : Any, InternalKey : Any, InternalValue : Any>(
    val simpleStore: SimpleBulkStore<InternalKey, InternalValue>,
    val keyMapping: Bijection<Key, InternalKey>,
    val valueMapping: Bijection<Value, InternalValue>,
    /**
     * If this returns false, the returned value is `null`
     */
    val exportEntry: (Pair<InternalKey, InternalValue>) -> Boolean = { true },
) : SimpleBulkStore<Key, Value> {
    override suspend fun get(keys: Set<Key>): Map<Key, Value?> = simpleStore.get(
        keys.map {
            keyMapping.forwards(it)
        }.toSet()
    ).filter { (key, value) ->
        value?.let {
            exportEntry(key to value)
        } ?: true
    }.mapValues {
        it.value?.let {
            valueMapping.backwards(it)
        }
    }.mapKeys {
        keyMapping.backwards(it.key)
    }

    override suspend fun put(entries: Map<Key, Value>): Map<Key, Value?> = simpleStore.put(
        entries.mapKeys {
            keyMapping.forwards(it.key)
        }.mapValues {
            valueMapping.forwards(it.value)
        }
    ).filter { (key, value) ->
        value?.let {
            exportEntry(key to value)
        } ?: true
    }.mapValues {
        it.value?.let {
            valueMapping.backwards(it)
        }
    }.mapKeys {
        keyMapping.backwards(it.key)
    }

    override suspend fun getOrPut(defaultValues: Map<Key, suspend () -> Value>): Map<Key, Value> = simpleStore.getOrPut(
        defaultValues.mapKeys {
            keyMapping.forwards(it.key)
        }.mapValues {
            {
                valueMapping.forwards(it.value())
            }
        }
    ).filter { (key, value) ->
        exportEntry(key to value)
    }.mapValues {
        valueMapping.backwards(it.value)
    }.mapKeys {
        keyMapping.backwards(it.key)
    }

    override suspend fun remove(keys: Collection<Key>): Map<Key, Value?> = simpleStore.remove(
        keys.map {
            keyMapping.forwards(it)
        }.toSet()
    ).filter { (key, value) ->
        value?.let {
            exportEntry(key to value)
        } ?: true
    }.mapValues {
        it.value?.let {
            valueMapping.backwards(it)
        }
    }.mapKeys {
        keyMapping.backwards(it.key)
    }


    override suspend fun keys(): Set<Key> = simpleStore.keys().map {
        keyMapping.backwards(it)
    }.toSet()

    override suspend fun entries(): Map<Key, Value> = simpleStore.entries().filter { (key, value) ->
        exportEntry(key to value)
    }.mapValues {
        valueMapping.backwards(it.value)
    }.mapKeys {
        keyMapping.backwards(it.key)
    }

    override suspend fun removeAllEntries() = simpleStore.removeAllEntries()
}