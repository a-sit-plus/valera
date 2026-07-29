package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.data.primitives.SimpleStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// ponytail: one process-wide lock keeps separate store instances coherent; split by DataStore only if contention appears.
private val persistentStoreMutex = Mutex()

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

    init {
        require(maxEntries == null || maxEntries > 0)
    }

    override suspend operator fun get(key: Key): Value? = persistentStoreMutex.withLock {
        loadEntries()[key]
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
        loadEntries().keys.toSet()
    }

    override suspend fun entries(): Map<Key, Value> = persistentStoreMutex.withLock {
        loadEntries()
    }

    override suspend fun remove(key: Key): Value? = persistentStoreMutex.withLock {
        loadEntries().run {
            remove(key).also { if (it != null) persistLocked(this) }
        }
    }

    override suspend fun removeAllEntries() = persistentStoreMutex.withLock {
        persistLocked(emptyMap())
    }

    // Deliberately NOT memoized: every op re-reads from DataStore. Several instances can target the same
    // preferenceKey (e.g. two PersistentHttpCacheStorage clients share `http_cache`), and reading fresh under
    // the shared [persistentStoreMutex] is what keeps them consistent — a per-instance memo cache would
    // reintroduce cross-instance clobber. (resetApp does not delete cache keys, so this is about concurrency.)
    private suspend fun loadEntries(): MutableMap<Key, Value> =
        catchingUnwrapped {
            dataStoreService.getPreference(preferenceKey).first()
                ?.let { json.decodeFromString(serializer, it).toMutableMap() }
                ?: mutableMapOf()
        }.onFailure {
            Napier.w("Could not load persistent cache '$preferenceKey'; starting empty", it)
        }.getOrDefault(mutableMapOf())

    private suspend fun persistLocked(entries: Map<Key, Value>) {
        catchingUnwrapped {
            if (entries.isEmpty()) {
                dataStoreService.deletePreference(preferenceKey)
            } else {
                dataStoreService.setPreference(
                    value = json.encodeToString(serializer, entries),
                    key = preferenceKey,
                )
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
