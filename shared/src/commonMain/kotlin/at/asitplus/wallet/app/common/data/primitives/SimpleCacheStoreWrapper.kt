package at.asitplus.wallet.app.common.data.primitives

import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Returns `nuLL` for expired values and emits them.
 * Does not actually remove elements from the underlying store.
 */
data class SimpleCacheStoreWrapper<Key : Any, Value : Any>(
    val store: SimpleBulkStore<Key, CacheStoreEntry<Value>>,
    val clock: Clock,
    val getCachingDuration: (Pair<Key, Value>) -> Duration,
    val onEntryFiltered: (Key) -> Unit,
) : SimpleBulkStore<Key, Value> by TransformingSimpleBulkStore<Key, Value, Key, CacheStoreEntry<Value>>(
    simpleStore = FilteringSimpleBulkStore(
        store,
        filter = { entry ->
            val (key, value) = entry
            val cachingDuration = getCachingDuration(key to value.data)
            val now = clock.now()
            (value.createdTime + cachingDuration > now).also {
                Napier.d("Reuse cached value for $key ($cachingDuration + ${value.createdTime} > $now): $it")
                if (!it) {
                    onEntryFiltered(key)
                }
            }
        }
    ),
    keyMapping = Bijection.identity(),
    valueMapping = object : Bijection<Value, CacheStoreEntry<Value>> {
        override fun forwards(domainElement: Value) = CacheStoreEntry(
            data = domainElement,
            createdTime = clock.now(),
        )

        override fun backwards(codomainElement: CacheStoreEntry<Value>) = codomainElement.data
    },
)