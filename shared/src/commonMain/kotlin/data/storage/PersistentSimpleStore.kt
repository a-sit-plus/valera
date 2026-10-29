package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.data.primitives.SimpleStore
import io.github.aakira.napier.Napier
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * One lock per preference key keeps separate store instances of the same key coherent, e.g. two
 * [PersistentHttpCacheStorage] clients sharing `http_cache`, while stores of different keys do not wait for each
 * other, e.g. a status list lookup for a write of the HTTP cache.
 */
private object PreferenceLocks : SynchronizedObject() {
    private val mutexes = mutableMapOf<String, Mutex>()

    fun of(preferenceKey: String): Mutex = synchronized(this) { mutexes.getOrPut(preferenceKey) { Mutex() } }
}

/**
 * DataStore-backed [SimpleStore] for caches. Invalid persisted data is ignored so callers can fetch it again.
 */
class PersistentSimpleStore<Key : Any, Value : Any>(
    private val dataStoreService: DataStoreService,
    private val preferenceKey: String,
    keySerializer: KSerializer<Key>,
    valueSerializer: KSerializer<Value>,
    private val json: Json = joseCompliantSerializer,
    private val maxEntries: Int? = null,
) : SimpleStore<Key, Value> {
    private val serializer = MapSerializer(keySerializer, valueSerializer)
    private val persistentStoreMutex = PreferenceLocks.of(preferenceKey)

    /** The entries decoded from [decodedFrom], the persisted value they were decoded from last, see [loadEntries]. */
    private var decodedFrom: String? = null
    private var decoded: Map<Key, Value> = emptyMap()

    init {
        require(maxEntries == null || maxEntries > 0)
    }

    override suspend operator fun get(key: Key): Value? = persistentStoreMutex.withLock {
        currentEntries()[key]
    }

    override suspend fun set(key: Key, value: Value): Value? = persistentStoreMutex.withLock {
        loadEntries().run {
            val previous = remove(key)
            this[key] = value
            trimLocked()
            persistLocked(this)
            previous
        }
    }

    override suspend fun getOrPut(key: Key, defaultValue: suspend () -> Value): Value =
        persistentStoreMutex.withLock {
            val entries = loadEntries()
            entries[key] ?: defaultValue().also {
                entries[key] = it
                entries.trimLocked()
                persistLocked(entries)
            }
        }

    override suspend fun keys(): Set<Key> = persistentStoreMutex.withLock {
        currentEntries().keys.toSet()
    }

    override suspend fun entries(): Map<Key, Value> = persistentStoreMutex.withLock {
        currentEntries().toMap()
    }

    override suspend fun remove(key: Key): Value? = persistentStoreMutex.withLock {
        loadEntries().run {
            remove(key).also { if (it != null) persistLocked(this) }
        }
    }

    override suspend fun removeAllEntries() = persistentStoreMutex.withLock {
        persistLocked(emptyMap())
    }

    // Every op re-reads the persisted value from DataStore: several instances can target the same preferenceKey
    // (e.g. two PersistentHttpCacheStorage clients share `http_cache`), and reading fresh under the lock of the key
    // is what keeps them consistent, also with the other process on iOS. Only decoding it is skipped while the value
    // is the one decoded last, as every status list lookup would decode every cached token otherwise. The entries
    // are shared, so they are not modified, see [loadEntries].
    private suspend fun currentEntries(): Map<Key, Value> =
        catchingUnwrapped {
            val stored = dataStoreService.getPreference(preferenceKey).first()
            if (stored == null) {
                remember(null, emptyMap())
            } else if (stored !== decodedFrom && stored != decodedFrom) {
                remember(stored, json.decodeFromString(serializer, stored))
            }
            decoded
        }.onFailure {
            Napier.w("Could not load persistent cache '$preferenceKey'; starting empty", it)
            remember(null, emptyMap())
        }.getOrDefault(emptyMap())

    /** A copy of [currentEntries] to modify and persist. */
    private suspend fun loadEntries(): MutableMap<Key, Value> = currentEntries().toMutableMap()

    private fun remember(stored: String?, entries: Map<Key, Value>) {
        decodedFrom = stored
        decoded = entries
    }

    private suspend fun persistLocked(entries: Map<Key, Value>) {
        catchingUnwrapped {
            if (entries.isEmpty()) {
                dataStoreService.deletePreference(preferenceKey)
                remember(null, emptyMap())
            } else {
                val encoded = json.encodeToString(serializer, entries)
                dataStoreService.setPreference(value = encoded, key = preferenceKey)
                remember(encoded, entries.toMap())
            }
        }.onFailure {
            Napier.w("Could not persist cache '$preferenceKey'", it)
        }
    }

    private fun MutableMap<Key, Value>.trimLocked() {
        while (maxEntries != null && size > maxEntries) {
            remove(keys.first())
        }
    }

    companion object {
        inline operator fun <reified Key : Any, reified Value : Any> invoke(
            dataStoreService: DataStoreService,
            preferenceKey: String,
            json: Json = joseCompliantSerializer,
            maxEntries: Int? = null,
        ) = PersistentSimpleStore(
            dataStoreService = dataStoreService,
            preferenceKey = preferenceKey,
            keySerializer = serializer<Key>(),
            valueSerializer = serializer<Value>(),
            json = json,
            maxEntries = maxEntries,
        )
    }
}
